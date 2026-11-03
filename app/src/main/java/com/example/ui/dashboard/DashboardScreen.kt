package com.example.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ai.AiDistractionPingService
import com.example.data.model.AppLimit
import com.example.data.model.DailyLog
import com.example.data.model.UserProfile
import com.example.service.InterceptManager
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CalmTeal
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.PermissionUtils
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    userProfile: UserProfile?,
    todayLog: DailyLog?,
    appLimits: List<AppLimit>,
    onToggleLimit: (AppLimit) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val aiPingService = remember { AiDistractionPingService() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAccessibilityOn by remember { mutableStateOf(PermissionUtils.isAccessibilityEnabled(context)) }
    var isOverlayOn by remember { mutableStateOf(PermissionUtils.isOverlayEnabled(context)) }

    // Re-check permissions when returning to the app from system Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityOn = PermissionUtils.isAccessibilityEnabled(context)
                isOverlayOn = PermissionUtils.isOverlayEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var aiPingMessage by remember {
        mutableStateOf("Opening apps out of boredom? Your ${userProfile?.currentStreak ?: 1}-day streak is watching. Mind every swipe.")
    }
    var isGeneratingPing by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            // Header Profile & Streak Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FocusFlow",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CalmTeal,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = userProfile?.displayName ?: "Digital Detox",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                // Streak Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF261900))
                        .border(1.dp, StreakAmber, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("streak_badge")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔥", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${userProfile?.currentStreak ?: 1} Day Streak",
                            color = StreakAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Live Protection & Permissions Status
        item {
            if (!isAccessibilityOn || !isOverlayOn) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261500)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StreakAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StreakAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Setup Required for Reels & App Blocking",
                                color = StreakAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Android requires explicit permission for FocusFlow to detect Reels inside Instagram and display the 60-second cooldown timer.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (!isAccessibilityOn) {
                            Button(
                                onClick = { PermissionUtils.openAccessibilitySettings(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = StreakAmber),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("enable_accessibility_button")
                            ) {
                                Text("1. Enable FocusFlow Accessibility Service", color = OledBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (!isOverlayOn) {
                            Button(
                                onClick = { PermissionUtils.openOverlaySettings(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("enable_overlay_button")
                            ) {
                                Text("2. Allow Display Over Other Apps (Overlay)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF00241E)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CalmTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CalmTeal, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Active Shield: Reels & Shorts Interceptor Armed",
                                color = CalmTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Instagram and YouTube rabbit holes will be intercepted with 60s cooldown.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Productivity & Screen Time Dial Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TODAY'S DISCIPLINE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp)
                    ) {
                        val score = userProfile?.todayProductivityScore ?: 85
                        val animatedProgress by animateFloatAsState(
                            targetValue = score / 100f,
                            label = "scoreProgress"
                        )

                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = DarkSurfaceVariant,
                            strokeWidth = 12.dp
                        )
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = CalmTeal,
                            strokeWidth = 12.dp
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$score",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "PRODUCTIVITY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricItem(
                            label = "Screen Time",
                            value = "${(todayLog?.totalScreenMinutes ?: 120) / 60}h ${(todayLog?.totalScreenMinutes ?: 120) % 60}m",
                            color = TextPrimary
                        )
                        MetricItem(
                            label = "Focus Time",
                            value = "${todayLog?.focusMinutes ?: 60}m",
                            color = CalmTeal
                        )
                        MetricItem(
                            label = "Saved",
                            value = "${userProfile?.totalScreenMinutesSaved ?: 35}m",
                            color = SuccessGreen
                        )
                    }
                }
            }
        }

        // Section Blocking Shields (Instagram Reels & YouTube Shorts)
        item {
            Text(
                text = "SURGICAL SECTION GUARDS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            val instaLimit = appLimits.find { it.packageName == "com.instagram.android" }
            val ytLimit = appLimits.find { it.packageName == "com.google.android.youtube" }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionGuardCard(
                    title = "Instagram Reels Guard",
                    subtitle = "Allows direct messages and posts; intercepts Reels tab with 60s cooldown.",
                    isActive = instaLimit?.blockReels ?: true,
                    interceptCount = todayLog?.reelsInterceptCount ?: 0,
                    onToggle = {
                        instaLimit?.let {
                            onToggleLimit(it.copy(blockReels = !it.blockReels))
                        }
                    }
                )

                SectionGuardCard(
                    title = "YouTube Shorts Guard",
                    subtitle = "Allows full video search & educational playlists; locks Shorts player.",
                    isActive = ytLimit?.blockShorts ?: true,
                    interceptCount = todayLog?.shortsInterceptCount ?: 0,
                    onToggle = {
                        ytLimit?.let {
                            onToggleLimit(it.copy(blockShorts = !it.blockShorts))
                        }
                    }
                )
            }
        }

        // AI Distraction Ping (Strict streak stakes)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CalmTeal.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CalmTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI DISTRACTION PING",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CalmTeal
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isGeneratingPing = true
                                    val ping = aiPingService.generateDistractionPing(
                                        appName = "Instagram",
                                        currentStreak = userProfile?.currentStreak ?: 3,
                                        timesOpenedToday = 9,
                                        minutesSpentToday = 48
                                    )
                                    aiPingMessage = ping
                                    isGeneratingPing = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isGeneratingPing,
                            modifier = Modifier.height(32.dp).testTag("generate_ai_ping_button")
                        ) {
                            if (isGeneratingPing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = CalmTeal,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Re-Ping", fontSize = 11.sp, color = CalmTeal)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "\"$aiPingMessage\"",
                        fontSize = 14.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Strict coaching active — powered by Gemini 3.8 Flash",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Live Intervention Simulator
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Live Feature Testing Sandbox",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Test the exact Reels 60-second grace cooldown or immediate app lockout now.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                InterceptManager.onSectionDetected(context, "com.instagram.android", "Instagram Reels")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("test_reels_intercept_button")
                        ) {
                            Text("Trigger Reels 60s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                InterceptManager.launchLockoutActivity(context, "com.instagram.android", "Instagram (Hard Restricted)")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30363D)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("test_instagram_lockout_button")
                        ) {
                            Text("Full App Lock", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun SectionGuardCard(
    title: String,
    subtitle: String,
    isActive: Boolean,
    interceptCount: Int,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) CalmTeal.copy(alpha = 0.5f) else DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (interceptCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$interceptCount intercepted",
                                color = CalmTeal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OledBlack,
                    checkedTrackColor = CalmTeal,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = DarkSurfaceVariant
                ),
                modifier = Modifier.testTag("switch_${title.replace(" ", "_").lowercase()}")
            )
        }
    }
}
