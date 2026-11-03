package com.example.ui.lockout

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.InterceptManager
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CalmTeal
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class LockoutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetPackage = intent.getStringExtra("TARGET_PACKAGE") ?: "Target App"
        val targetSection = intent.getStringExtra("TARGET_SECTION") ?: "Shorts / Reels"

        setContent {
            MyApplicationTheme {
                LockoutScreen(
                    targetPackage = targetPackage,
                    targetSection = targetSection,
                    onExitToHome = {
                        InterceptManager.resetCooldown()
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    },
                    onEmergencyBypass = {
                        InterceptManager.activateEmergencyBypass(this@LockoutActivity)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun LockoutScreen(
    targetPackage: String,
    targetSection: String,
    onExitToHome: () -> Unit,
    onEmergencyBypass: () -> Unit
) {
    var showBypassDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = "Shield Lockout",
                tint = AlertCrimson,
                modifier = Modifier
                    .size(80.dp)
                    .testTag("lockout_shield_icon")
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SESSION TERMINATED",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AlertCrimson,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$targetSection blocked by FocusFlow",
                fontSize = 16.sp,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "The 60-second grace cooldown has expired.\nProtect your streak. Close this app and return to what matters.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔥 Streak Stake Active",
                        color = Color(0xFFFF9500),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bypassing this lockout will mark an intentional lapse on today's report.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onExitToHome,
                colors = ButtonDefaults.buttonColors(containerColor = CalmTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("exit_to_home_button")
            ) {
                Text(
                    text = "Exit to Home Screen",
                    color = OledBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showBypassDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertCrimson),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("emergency_bypass_button")
            ) {
                Text("Emergency Bypass (Strict)")
            }
        }
    }

    if (showBypassDialog) {
        val requiredPhrase = "I choose to pause my streak"
        var typedPhrase by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showBypassDialog = false },
            containerColor = DarkSurfaceVariant,
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = AlertCrimson)
            },
            title = {
                Text("Emergency Bypass Confirmation", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "To bypass for 15 minutes, type the exact phrase below:\n\"$requiredPhrase\"",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = typedPhrase,
                        onValueChange = { typedPhrase = it },
                        singleLine = true,
                        placeholder = { Text("Type here...", color = TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bypass_phrase_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBypassDialog = false
                        onEmergencyBypass()
                    },
                    enabled = typedPhrase.trim().equals(requiredPhrase, ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                    modifier = Modifier.testTag("confirm_bypass_button")
                ) {
                    Text("Unlock 15m")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBypassDialog = false }) {
                    Text("Cancel", color = CalmTeal)
                }
            }
        )
    }
}
