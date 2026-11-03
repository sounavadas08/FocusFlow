package com.example.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.model.AppLimit
import com.example.ui.lockout.LockoutActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CooldownState {
    data object Idle : CooldownState
    data class Active(
        val packageName: String,
        val sectionName: String,
        val secondsRemaining: Int,
        val totalSeconds: Int = 60
    ) : CooldownState
    data class HardLocked(
        val packageName: String,
        val sectionName: String
    ) : CooldownState
}

object InterceptManager {

    private const val TAG = "InterceptManager"
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var countdownJob: Job? = null

    private val _cooldownState = MutableStateFlow<CooldownState>(CooldownState.Idle)
    val cooldownState: StateFlow<CooldownState> = _cooldownState.asStateFlow()

    private var onInterceptionRecorded: ((Boolean) -> Unit)? = null
    private var bypassExpiresAtMillis: Long = 0L

    // In-memory cache of configured app limits
    private val limitsMap = mutableMapOf<String, AppLimit>()

    fun setAppLimits(limits: List<AppLimit>) {
        limitsMap.clear()
        limits.forEach { limitsMap[it.packageName] = it }
        Log.d(TAG, "Loaded ${limits.size} app limits into InterceptManager")
    }

    fun isEmergencyBypassActive(): Boolean {
        return System.currentTimeMillis() < bypassExpiresAtMillis
    }

    fun setInterceptionCallback(callback: (Boolean) -> Unit) {
        onInterceptionRecorded = callback
    }

    /**
     * Checks if the entire application is restricted/hard-blocked.
     */
    fun checkAppAccess(context: Context, packageName: String): Boolean {
        if (isEmergencyBypassActive()) return true

        val limit = limitsMap[packageName] ?: return true
        if (limit.isBlocked) {
            Log.w(TAG, "Application $packageName is hard-blocked. Triggering lockout.")
            launchLockoutActivity(context, packageName, "${limit.appName} (Daily Limit Exceeded)")
            return false
        }
        return true
    }

    /**
     * Triggered when AccessibilityService detects Reels or YouTube Shorts.
     */
    fun onSectionDetected(context: Context, packageName: String, sectionName: String) {
        if (isEmergencyBypassActive()) {
            Log.d(TAG, "Emergency bypass is active, ignoring $sectionName")
            return
        }

        val limit = limitsMap[packageName]
        val isReels = sectionName.contains("Reels", ignoreCase = true)
        val isShorts = sectionName.contains("Shorts", ignoreCase = true)

        // Check if section blocking is explicitly enabled for this app
        if (limit != null) {
            if (isReels && !limit.blockReels) return
            if (isShorts && !limit.blockShorts) return
            if (limit.isBlocked) {
                launchLockoutActivity(context, packageName, "${limit.appName} (Restricted)")
                return
            }
        }

        val current = _cooldownState.value
        if (current is CooldownState.HardLocked && current.packageName == packageName) {
            launchLockoutActivity(context, packageName, sectionName)
            return
        }

        if (current is CooldownState.Active && current.packageName == packageName) {
            return
        }

        // Start 60-second cooldown
        val cooldownSecs = limit?.cooldownSeconds ?: 60
        startCooldown(context, packageName, sectionName, cooldownSecs)
        onInterceptionRecorded?.invoke(isReels)
    }

    fun startCooldown(context: Context, packageName: String, sectionName: String, durationSeconds: Int = 60) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (sec in durationSeconds downTo 1) {
                _cooldownState.value = CooldownState.Active(
                    packageName = packageName,
                    sectionName = sectionName,
                    secondsRemaining = sec,
                    totalSeconds = durationSeconds
                )
                delay(1000L)
            }
            // Transition to Hard Lockout
            _cooldownState.value = CooldownState.HardLocked(packageName, sectionName)
            launchLockoutActivity(context, packageName, sectionName)
        }
    }

    fun launchLockoutActivity(context: Context, packageName: String, sectionName: String) {
        val intent = Intent(context, LockoutActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TARGET_PACKAGE", packageName)
            putExtra("TARGET_SECTION", sectionName)
        }
        context.startActivity(intent)
    }

    fun resetCooldown() {
        countdownJob?.cancel()
        _cooldownState.value = CooldownState.Idle
    }

    fun activateEmergencyBypass(context: Context, durationMinutes: Int = 15) {
        countdownJob?.cancel()
        _cooldownState.value = CooldownState.Idle
        bypassExpiresAtMillis = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(homeIntent)
    }
}
