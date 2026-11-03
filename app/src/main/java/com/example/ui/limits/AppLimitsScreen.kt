package com.example.ui.limits

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLimit
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CalmTeal
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun AppLimitsScreen(
    appLimits: List<AppLimit>,
    onSaveLimit: (AppLimit) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "App & Section Limits",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Set daily quotas and selectively disable short-form algorithmic loops like Reels or Shorts.",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        items(appLimits, key = { it.packageName }) { limit ->
            AppLimitCard(
                limit = limit,
                onUpdate = onSaveLimit
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AppLimitCard(
    limit: AppLimit,
    onUpdate: (AppLimit) -> Unit
) {
    var budgetMinutes by remember(limit.dailyBudgetMinutes) {
        mutableFloatStateOf(limit.dailyBudgetMinutes.toFloat())
    }
    var blockReels by remember(limit.blockReels) { mutableStateOf(limit.blockReels) }
    var blockShorts by remember(limit.blockShorts) { mutableStateOf(limit.blockShorts) }
    var isHardBlocked by remember(limit.isBlocked) { mutableStateOf(limit.isBlocked) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isHardBlocked) AlertCrimson.copy(alpha = 0.6f) else DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // App Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = CalmTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = limit.appName,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isHardBlocked) "Hard Blocked" else "${budgetMinutes.roundToInt()} min daily quota",
                            color = if (isHardBlocked) AlertCrimson else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Hard block toggle
                Switch(
                    checked = isHardBlocked,
                    onCheckedChange = { checked ->
                        isHardBlocked = checked
                        onUpdate(limit.copy(isBlocked = checked))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AlertCrimson,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.testTag("block_${limit.packageName}")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Daily Quota Slider
            if (!isHardBlocked) {
                Text(
                    text = "Daily Limit: ${budgetMinutes.roundToInt()} minutes",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = budgetMinutes,
                    onValueChange = { budgetMinutes = it },
                    onValueChangeFinished = {
                        onUpdate(limit.copy(dailyBudgetMinutes = budgetMinutes.roundToInt()))
                    },
                    valueRange = 10f..180f,
                    steps = 16,
                    colors = SliderDefaults.colors(
                        thumbColor = CalmTeal,
                        activeTrackColor = CalmTeal,
                        inactiveTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.testTag("slider_${limit.packageName}")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Section Specific Blocking
                if (limit.packageName == "com.instagram.android") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Block Reels Section Only",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enforces 60-second cooldown timer before locking Reels.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = blockReels,
                            onCheckedChange = { checked ->
                                blockReels = checked
                                onUpdate(limit.copy(blockReels = checked))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OledBlack,
                                checkedTrackColor = CalmTeal
                            )
                        )
                    }
                }

                if (limit.packageName == "com.google.android.youtube") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Block Shorts Player Only",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Intercepts endless scrolling while keeping long videos active.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = blockShorts,
                            onCheckedChange = { checked ->
                                blockShorts = checked
                                onUpdate(limit.copy(blockShorts = checked))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OledBlack,
                                checkedTrackColor = CalmTeal
                            )
                        )
                    }
                }
            }
        }
    }
}
