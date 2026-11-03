package com.example.ui.focus

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.data.model.FilteredNotification
import com.example.ui.theme.CalmTeal
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun PomodoroScreen() {
    val context = LocalContext.current
    var sessionDurationMinutes by remember { mutableIntStateOf(25) }
    var secondsRemaining by remember { mutableIntStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var isBreakMode by remember { mutableStateOf(false) }
    var autoDndEnabled by remember { mutableStateOf(true) }

    // Batch notification tray holding notifications during focus mode
    val sampleFilteredNotifications = remember {
        mutableListOf(
            FilteredNotification("1", "com.instagram.android", "Instagram", "alex_99 liked your story"),
            FilteredNotification("2", "com.google.android.youtube", "YouTube", "New Shorts from your subscriptions"),
            FilteredNotification("3", "com.twitter.android", "X", "Trending in Tech: Android 16 features")
        )
    }

    LaunchedEffect(isRunning, secondsRemaining) {
        if (isRunning && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
        } else if (isRunning && secondsRemaining == 0) {
            isRunning = false
            if (!isBreakMode) {
                // Switch to 5 min break
                isBreakMode = true
                secondsRemaining = 5 * 60
            } else {
                // Back to focus
                isBreakMode = false
                secondsRemaining = sessionDurationMinutes * 60
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Focus Mode & Pomodoro",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Distraction-free deep work sessions with automatic DND and notification filtering.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Circular Timer Display
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Session Mode Tabs
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = !isBreakMode,
                            onClick = {
                                if (!isRunning) {
                                    isBreakMode = false
                                    secondsRemaining = sessionDurationMinutes * 60
                                }
                            },
                            label = { Text("Deep Work (25m)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CalmTeal,
                                selectedLabelColor = OledBlack,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )

                        FilterChip(
                            selected = isBreakMode,
                            onClick = {
                                if (!isRunning) {
                                    isBreakMode = true
                                    secondsRemaining = 5 * 60
                                }
                            },
                            label = { Text("Rest Break (5m)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = OledBlack,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(200.dp)
                    ) {
                        val totalSecs = if (isBreakMode) 5 * 60 else sessionDurationMinutes * 60
                        val progress = secondsRemaining.toFloat() / totalSecs.toFloat()
                        val animatedProgress by animateFloatAsState(targetValue = progress, label = "pomodoro")

                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = DarkSurfaceVariant,
                            strokeWidth = 14.dp
                        )
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = if (isBreakMode) ElectricCyan else CalmTeal,
                            strokeWidth = 14.dp
                        )

                        val minutes = secondsRemaining / 60
                        val seconds = secondsRemaining % 60
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%02d:%02d", minutes, seconds),
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isRunning) (if (isBreakMode) "RECHARGING" else "FOCUSING") else "READY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBreakMode) ElectricCyan else CalmTeal,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Timer Controls
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isRunning = false
                                secondsRemaining = if (isBreakMode) 5 * 60 else sessionDurationMinutes * 60
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Timer", tint = TextSecondary)
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Button(
                            onClick = { isRunning = !isRunning },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) Color(0xFFFF453A) else CalmTeal
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(68.dp)
                                .testTag("play_pause_timer_button")
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "Pause" else "Start",
                                tint = OledBlack,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }
        }

        // Auto DND Toggle Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoNotDisturbOn,
                                contentDescription = null,
                                tint = CalmTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Auto DND Schedule",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Silences incoming alerts automatically while session is active.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = autoDndEnabled,
                        onCheckedChange = { autoDndEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OledBlack,
                            checkedTrackColor = CalmTeal
                        ),
                        modifier = Modifier.testTag("auto_dnd_switch")
                    )
                }
            }
        }

        // Notification Filtering Tray
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsPaused,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FILTERED NOTIFICATION TRAY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "${sampleFilteredNotifications.size} Held",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(sampleFilteredNotifications) { notif ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notif.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = notif.message,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "Held for review",
                        color = CalmTeal,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
