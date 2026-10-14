package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.TrackingState
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FamilyManagementScreen
import com.example.ui.screens.FamilySetupScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.viewmodel.MainViewModel

enum class MainNavTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Home),
    MAP("Map", Icons.Default.Map),
    HISTORY("History", Icons.Default.History),
    FAMILY("Family", Icons.Default.Group),
    SETTINGS("Settings", Icons.Default.Settings)
}

/**
 * Highly visible, elegant top header featuring app identity and the "Made by Fahmid" watermark badge.
 * Includes statusBarsPadding to ensure it is never hidden behind the system notch or status bar icons.
 */
@Composable
fun FamTrackTopBar(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "FamTrack Logo",
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FamTrack",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Clearly visible, prominent "Made by Fahmid" badge
            Surface(
                color = CyanAccent.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.2.dp, CyanAccent.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Made by Fahmid",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.4.sp
                        ),
                        color = CyanAccent
                    )
                }
            }
        }
    }
}

@Composable
fun FamTrackApp(viewModel: MainViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val currentFamily by viewModel.currentFamily.collectAsStateWithLifecycle()
    val familyMembers by viewModel.familyMembers.collectAsStateWithLifecycle()
    val trackingState by viewModel.trackingState.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val latestLocation by viewModel.latestLocation.collectAsStateWithLifecycle()
    val locationHistory by viewModel.locationHistory.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(MainNavTab.DASHBOARD) }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    when {
        // Step 1: Sign In / Create Local Profile
        userProfile == null -> {
            Column(modifier = Modifier.fillMaxSize()) {
                FamTrackTopBar()
                AuthScreen(
                    onSignIn = { name -> viewModel.signIn(name) }
                )
            }
        }

        // Step 2: Explicit Consent & Permission Onboarding
        !userProfile!!.hasConsentedToTracking -> {
            Column(modifier = Modifier.fillMaxSize()) {
                FamTrackTopBar()
                OnboardingScreen(
                    onConsentGiven = {
                        viewModel.grantConsent()
                    }
                )
            }
        }

        // Step 3: Create or Join Family Group
        userProfile!!.familyId == null || currentFamily == null -> {
            Column(modifier = Modifier.fillMaxSize()) {
                FamTrackTopBar()
                FamilySetupScreen(
                    userName = userProfile!!.displayName,
                    onCreateFamily = { name -> viewModel.createFamily(name) },
                    onJoinFamily = { code -> viewModel.joinFamily(code) }
                )
            }
        }

        // Step 4: Main Family Navigation Container
        else -> {
            val family = currentFamily!!
            val user = userProfile!!

            // Smooth Back navigation: return to Dashboard before exiting app
            BackHandler(enabled = selectedTab != MainNavTab.DASHBOARD) {
                selectedTab = MainNavTab.DASHBOARD
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    FamTrackTopBar()
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        MainNavTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanAccent,
                                    selectedTextColor = CyanAccent,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (selectedTab) {
                        MainNavTab.DASHBOARD -> {
                            DashboardScreen(
                                family = family,
                                currentUserId = user.uid,
                                members = familyMembers,
                                trackingState = trackingState,
                                syncStatus = syncStatus,
                                latestLocation = latestLocation,
                                onStartSharing = { viewModel.startSharing() },
                                onStopSharing = { viewModel.stopSharing() },
                                onManualSync = { viewModel.triggerSync() },
                                onNavigateToMap = { selectedTab = MainNavTab.MAP }
                            )
                        }
                        MainNavTab.MAP -> {
                            MapScreen(
                                currentUserId = user.uid,
                                members = familyMembers,
                                selfLatestLocation = latestLocation
                            )
                        }
                        MainNavTab.HISTORY -> {
                            HistoryScreen(
                                history = locationHistory,
                                onTriggerSync = { viewModel.triggerSync() },
                                onClearHistory = { viewModel.clearLocalHistory() }
                            )
                        }
                        MainNavTab.FAMILY -> {
                            FamilyManagementScreen(
                                family = family,
                                currentUserId = user.uid,
                                members = familyMembers,
                                onLeaveFamily = { viewModel.leaveFamily() }
                            )
                        }
                        MainNavTab.SETTINGS -> {
                            SettingsScreen(
                                userProfile = user,
                                trackingState = trackingState,
                                syncStatus = syncStatus,
                                onUpdateRetention = { days -> viewModel.updateRetentionDays(days) },
                                onManualSync = { viewModel.triggerSync() },
                                onClearHistory = { viewModel.clearLocalHistory() }
                            )
                        }
                    }
                }
            }
        }
    }
}
