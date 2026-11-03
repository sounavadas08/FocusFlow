package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppLimit
import com.example.data.model.DailyLog
import com.example.data.model.MilestoneBadge
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException

class FocusRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance(db.app)

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(UserProfile::class.java)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun getOrCreateProfile(displayName: String): UserProfile {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            return snapshot.toObject(UserProfile::class.java) ?: UserProfile(id = uid, userId = uid, displayName = displayName)
        }
        val newProfile = UserProfile(
            id = uid,
            userId = uid,
            displayName = displayName.ifBlank { "Focus Warrior" },
            currentStreak = 1,
            bestStreak = 1,
            totalScreenMinutesSaved = 30,
            todayProductivityScore = 88
        )
        val payload = mapOf(
            "userId" to uid,
            "displayName" to newProfile.displayName,
            "currentStreak" to newProfile.currentStreak,
            "bestStreak" to newProfile.bestStreak,
            "totalScreenMinutesSaved" to newProfile.totalScreenMinutesSaved,
            "todayProductivityScore" to newProfile.todayProductivityScore,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        initDefaultLimits(uid)
        initDefaultMilestones(uid)
        return newProfile
    }

    suspend fun updateProductivityScore(score: Int, streakIncrement: Boolean = false) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val updates = mutableMapOf<String, Any>(
                "todayProductivityScore" to score.coerceIn(0, 100),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (streakIncrement) {
                updates["currentStreak"] = FieldValue.increment(1)
            }
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    fun observeAppLimits(): Flow<List<AppLimit>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/app_limits"
        emitAll(
            db.collection("users").document(uid).collection("app_limits")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(AppLimit::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveAppLimit(appLimit: AppLimit) {
        val uid = requireUserId()
        val docId = if (appLimit.id.isNotBlank()) appLimit.id else appLimit.packageName.replace(".", "_")
        val docRef = db.collection("users").document(uid).collection("app_limits").document(docId)
        val snapshot = docRef.get().await()
        val existingCreatedAt = if (snapshot.exists()) snapshot.getTimestamp("createdAt") else null

        val payload = mapOf(
            "id" to docId,
            "userId" to uid,
            "packageName" to appLimit.packageName,
            "appName" to appLimit.appName,
            "dailyBudgetMinutes" to appLimit.dailyBudgetMinutes,
            "blockReels" to appLimit.blockReels,
            "blockShorts" to appLimit.blockShorts,
            "isBlocked" to appLimit.isBlocked,
            "cooldownSeconds" to appLimit.cooldownSeconds,
            "createdAt" to (existingCreatedAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
    }

    fun observeDailyLogs(): Flow<List<DailyLog>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/daily_logs"
        emitAll(
            db.collection("users").document(uid).collection("daily_logs")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(DailyLog::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getTodayLog(): DailyLog {
        val uid = requireUserId()
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val docRef = db.collection("users").document(uid).collection("daily_logs").document(todayStr)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            return snapshot.toObject(DailyLog::class.java) ?: DailyLog(id = todayStr, userId = uid, date = todayStr)
        }
        val initialLog = DailyLog(
            id = todayStr,
            userId = uid,
            date = todayStr,
            totalScreenMinutes = 120,
            focusMinutes = 60,
            distractedMinutes = 60,
            productivityScore = 80,
            reelsInterceptCount = 0,
            shortsInterceptCount = 0,
            emergencyBypassCount = 0
        )
        val payload = mapOf(
            "id" to todayStr,
            "userId" to uid,
            "date" to todayStr,
            "totalScreenMinutes" to initialLog.totalScreenMinutes,
            "focusMinutes" to initialLog.focusMinutes,
            "distractedMinutes" to initialLog.distractedMinutes,
            "productivityScore" to initialLog.productivityScore,
            "reelsInterceptCount" to initialLog.reelsInterceptCount,
            "shortsInterceptCount" to initialLog.shortsInterceptCount,
            "emergencyBypassCount" to initialLog.emergencyBypassCount,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        return initialLog
    }

    suspend fun recordInterception(isReels: Boolean) {
        val uid = requireUserId()
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val docRef = db.collection("users").document(uid).collection("daily_logs").document(todayStr)
        try {
            getTodayLog() // Ensure document exists
            val field = if (isReels) "reelsInterceptCount" else "shortsInterceptCount"
            docRef.update(
                mapOf(
                    field to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Also increment total minutes saved in profile (~5 mins saved per doomscroll loop intercepted)
            db.collection("users").document(uid).update(
                mapOf(
                    "totalScreenMinutesSaved" to FieldValue.increment(5),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun recordEmergencyBypass() {
        val uid = requireUserId()
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val docRef = db.collection("users").document(uid).collection("daily_logs").document(todayStr)
        try {
            getTodayLog()
            docRef.update(
                mapOf(
                    "emergencyBypassCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    fun observeMilestones(): Flow<List<MilestoneBadge>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/milestones"
        emitAll(
            db.collection("users").document(uid).collection("milestones")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(MilestoneBadge::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    private suspend fun initDefaultLimits(uid: String) {
        val defaults = listOf(
            AppLimit(id = "com_instagram_android", userId = uid, packageName = "com.instagram.android", appName = "Instagram", dailyBudgetMinutes = 30, blockReels = true, blockShorts = false, isBlocked = false, cooldownSeconds = 60),
            AppLimit(id = "com_google_android_youtube", userId = uid, packageName = "com.google.android.youtube", appName = "YouTube", dailyBudgetMinutes = 45, blockReels = false, blockShorts = true, isBlocked = false, cooldownSeconds = 60),
            AppLimit(id = "com_zhiliaoapp_musically", userId = uid, packageName = "com.zhiliaoapp.musically", appName = "TikTok", dailyBudgetMinutes = 20, blockReels = true, blockShorts = true, isBlocked = true, cooldownSeconds = 60),
            AppLimit(id = "com_twitter_android", userId = uid, packageName = "com.twitter.android", appName = "X / Twitter", dailyBudgetMinutes = 30, blockReels = false, blockShorts = false, isBlocked = false, cooldownSeconds = 60),
            AppLimit(id = "com_reddit_frontpage", userId = uid, packageName = "com.reddit.frontpage", appName = "Reddit", dailyBudgetMinutes = 30, blockReels = false, blockShorts = false, isBlocked = false, cooldownSeconds = 60)
        )
        for (limit in defaults) {
            saveAppLimit(limit)
        }
    }

    private suspend fun initDefaultMilestones(uid: String) {
        val badges = listOf(
            MilestoneBadge(id = "zen_novice", userId = uid, badgeId = "zen_novice", title = "Zen Novice", description = "Maintained a 3-day clean streak without exceeding app budgets.", iconName = "self_improvement", category = "Streak", unlocked = true),
            MilestoneBadge(id = "reels_resistor", userId = uid, badgeId = "reels_resistor", title = "Reels Resistor", description = "Intercepted 10 Instagram Reels loops with the 60s cooldown.", iconName = "shield", category = "Interception", unlocked = true),
            MilestoneBadge(id = "shorts_slayer", userId = uid, badgeId = "shorts_slayer", title = "Shorts Slayer", description = "Blocked YouTube Shorts 15 times and reclaimed deep focus.", iconName = "bolt", category = "Interception", unlocked = false),
            MilestoneBadge(id = "deep_worker", userId = uid, badgeId = "deep_worker", title = "Deep Work Master", description = "Completed 5 Pomodoro focus sessions in a single day.", iconName = "timer", category = "Focus", unlocked = false),
            MilestoneBadge(id = "iron_will", userId = uid, badgeId = "iron_will", title = "Iron Will", description = "7 consecutive days with zero emergency bypasses activated.", iconName = "lock", category = "Streak", unlocked = false)
        )
        for (b in badges) {
            val docRef = db.collection("users").document(uid).collection("milestones").document(b.id)
            val payload = mapOf(
                "id" to b.id,
                "userId" to uid,
                "badgeId" to b.badgeId,
                "title" to b.title,
                "description" to b.description,
                "iconName" to b.iconName,
                "category" to b.category,
                "unlocked" to b.unlocked
            )
            docRef.set(payload).await()
        }
    }
}
