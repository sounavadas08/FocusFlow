package com.example.service

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureOverlaySetup()
        observeCooldown()
        startForegroundAppWatcher()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureOverlaySetup()
        return START_STICKY
    }

    private fun ensureOverlaySetup() {
        if (overlayView == null && Settings.canDrawOverlays(this)) {
            setupOverlay()
        }
    }

    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 80
        }

        val hud = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setBackgroundColor(android.graphics.Color.parseColor("#F2000000"))
            setPadding(36, 28, 36, 28)
            gravity = Gravity.CENTER_VERTICAL
            elevation = 20f
        }

        val text = TextView(this).apply {
            id = View.generateViewId()
            setTextColor(android.graphics.Color.parseColor("#00E5BC"))
            textSize = 14f
            setText("Reels Guard: 60s cooldown active")
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val exitBtn = Button(this).apply {
            setText("Exit App")
            setTextColor(android.graphics.Color.BLACK)
            setBackgroundColor(android.graphics.Color.parseColor("#00E5BC"))
            setOnClickListener {
                InterceptManager.resetCooldown()
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
            }
        }

        hud.addView(text)
        hud.addView(exitBtn)
        overlayView = hud

        try {
            windowManager?.addView(overlayView, params)
            overlayView?.visibility = View.GONE
        } catch (e: Exception) {
            overlayView = null
        }
    }

    private fun observeCooldown() {
        scope.launch {
            InterceptManager.cooldownState.collectLatest { state ->
                ensureOverlaySetup()
                when (state) {
                    is CooldownState.Active -> {
                        overlayView?.visibility = View.VISIBLE
                        (overlayView as? android.widget.LinearLayout)?.getChildAt(0)?.let { tv ->
                            if (tv is TextView) {
                                tv.setText("⚠️ ${state.sectionName}: ${state.secondsRemaining}s before hard lock!")
                            }
                        }
                    }
                    else -> {
                        overlayView?.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun startForegroundAppWatcher() {
        scope.launch(Dispatchers.Default) {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            while (isActive) {
                try {
                    val now = System.currentTimeMillis()
                    val stats = usageStatsManager?.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        now - 5000,
                        now
                    )
                    val topApp = stats?.maxByOrNull { it.lastTimeUsed }?.packageName
                    if (!topApp.isNullOrBlank() && topApp != packageName) {
                        launch(Dispatchers.Main) {
                            InterceptManager.checkAppAccess(this@FocusOverlayService, topApp)
                        }
                    }
                } catch (_: Exception) {}
                delay(1200L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
    }
}
