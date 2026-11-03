package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.model.AppLimit
import com.example.data.model.DailyLog
import com.example.data.model.MilestoneBadge
import com.example.data.model.UserProfile
import com.example.data.repository.FocusRepository
import com.example.service.FocusOverlayService
import com.example.service.InterceptManager
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.focus.PomodoroScreen
import com.example.ui.limits.AppLimitsScreen
import com.example.ui.milestones.MilestonesScreen
import com.example.ui.stats.StatisticsScreen
import com.example.ui.theme.CalmTeal
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Mandatory App Check configuration with intent
        configureAppCheck(this, intent)

        // Start overlay service if permission is enabled
        try {
            startService(Intent(this, FocusOverlayService::class.java))
        } catch (_: Exception) {}

        setContent {
            MyApplicationTheme {
                MainContent()
            }
        }
    }
}

enum class NavigationDestination(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Shield),
    LIMITS("Limits", Icons.Default.LockClock),
    FOCUS("Focus", Icons.Default.HourglassTop),
    ANALYTICS("Analytics", Icons.Default.BarChart),
    MILESTONES("Badges", Icons.Default.MilitaryTech)
}

@Composable
fun MainContent() {
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        AuthenticatedFocusApp(user = currentUser!!)
    }
}

@Composable
fun AuthenticatedFocusApp(user: FirebaseUser) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { FocusRepository(context) }
    val credentialManager = remember { CredentialManager.create(context) }

    var selectedTab by remember { mutableStateOf(NavigationDestination.DASHBOARD) }

    val userProfile by repository.observeProfile().collectAsState(initial = null)
    val appLimits by repository.observeAppLimits().collectAsState(initial = emptyList())
    val dailyLogs by repository.observeDailyLogs().collectAsState(initial = emptyList())
    val milestones by repository.observeMilestones().collectAsState(initial = emptyList())
    var todayLog by remember { mutableStateOf<DailyLog?>(null) }

    LaunchedEffect(appLimits) {
        InterceptManager.setAppLimits(appLimits)
    }

    LaunchedEffect(user.uid) {
        repository.getOrCreateProfile(user.displayName ?: "Focus Warrior")
        todayLog = repository.getTodayLog()

        // Link InterceptManager events to Firestore persistence
        InterceptManager.setInterceptionCallback { isReels ->
            coroutineScope.launch {
                repository.recordInterception(isReels)
                todayLog = repository.getTodayLog()
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                NavigationDestination.entries.forEach { dest ->
                    val isSelected = selectedTab == dest
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = dest },
                        icon = {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = dest.label,
                                tint = if (isSelected) CalmTeal else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = dest.label,
                                color = if (isSelected) CalmTeal else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFF00382E)
                        ),
                        modifier = Modifier.testTag("nav_${dest.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(OledBlack)
        ) {
            when (selectedTab) {
                NavigationDestination.DASHBOARD -> {
                    DashboardScreen(
                        userProfile = userProfile,
                        todayLog = todayLog,
                        appLimits = appLimits,
                        onToggleLimit = { limit ->
                            coroutineScope.launch { repository.saveAppLimit(limit) }
                        },
                        onSignOut = {
                            signOutUser(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = {},
                                scope = coroutineScope
                            )
                        }
                    )
                }
                NavigationDestination.LIMITS -> {
                    AppLimitsScreen(
                        appLimits = appLimits,
                        onSaveLimit = { limit ->
                            coroutineScope.launch { repository.saveAppLimit(limit) }
                        }
                    )
                }
                NavigationDestination.FOCUS -> {
                    PomodoroScreen()
                }
                NavigationDestination.ANALYTICS -> {
                    StatisticsScreen(
                        userProfile = userProfile,
                        dailyLogs = dailyLogs
                    )
                }
                NavigationDestination.MILESTONES -> {
                    MilestonesScreen(
                        userProfile = userProfile,
                        milestones = milestones
                    )
                }
            }
        }
    }
}
