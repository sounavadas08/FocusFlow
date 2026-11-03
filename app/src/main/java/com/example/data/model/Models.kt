package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "Focus Warrior",
    val currentStreak: Int = 1,
    val bestStreak: Int = 1,
    val totalScreenMinutesSaved: Int = 45,
    val todayProductivityScore: Int = 85,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppLimit(
    val id: String = "",
    val userId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val dailyBudgetMinutes: Int = 30,
    val blockReels: Boolean = true,
    val blockShorts: Boolean = true,
    @get:PropertyName("isBlocked") val isBlocked: Boolean = false,
    val cooldownSeconds: Int = 60,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailyLog(
    val id: String = "",
    val userId: String = "",
    val date: String = "",
    val totalScreenMinutes: Int = 180,
    val focusMinutes: Int = 90,
    val distractedMinutes: Int = 90,
    val productivityScore: Int = 75,
    val reelsInterceptCount: Int = 0,
    val shortsInterceptCount: Int = 0,
    val emergencyBypassCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MilestoneBadge(
    val id: String = "",
    val userId: String = "",
    val badgeId: String = "",
    val title: String = "",
    val description: String = "",
    val iconName: String = "shield",
    val category: String = "Streak",
    val unlocked: Boolean = false,
    @ServerTimestamp val unlockedAt: Timestamp? = null
)

data class FilteredNotification(
    val id: String,
    val packageName: String,
    val title: String,
    val message: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
