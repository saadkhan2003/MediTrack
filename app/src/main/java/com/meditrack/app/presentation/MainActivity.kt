package com.meditrack.app.presentation

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.meditrack.app.BuildConfig
import com.meditrack.app.data.preferences.AppPreferences
import com.meditrack.app.data.sync.FirestoreSyncService
import com.meditrack.app.presentation.components.BottomNavBar
import com.meditrack.app.presentation.navigation.NavGraph
import com.meditrack.app.presentation.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.meditrack.app.presentation.theme.MediTrackTheme



@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var firestoreSyncService: FirestoreSyncService

    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var observedAuthUid: String? = null
    private var authStateInitialized: Boolean = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — app continues either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()
        checkExactAlarmPermission()
        promptBatteryOptimizationExemptionIfNeeded()
        observeAuthStateForRealtimeSync()

        setContent {
            val prefs by appPreferences.state.collectAsState(initial = null)
            val prefState = prefs

            if (prefState == null) return@setContent

            MediTrackTheme(
                themeMode = prefState.themeMode,
                fontSize = prefState.fontSize
            ) {
                MediTrackApp(
                    initialHighlightMedicineId = intent?.getIntExtra("highlightMedicineId", -1)?.takeIf { it > 0 }
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    // Rationale handled by platform callback; launch request afterwards.
                }
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    private fun promptBatteryOptimizationExemptionIfNeeded() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
            lifecycleScope.launch {
                val state = appPreferences.state.first()
                if (!state.batteryOptimizationPromptShown) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    appPreferences.setBatteryOptimizationPromptShown(true)
                }
            }
        }
    }

    private fun observeAuthStateForRealtimeSync() {
        if (!BuildConfig.FEATURE_FIREBASE) return
        val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull() ?: return

        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            lifecycleScope.launch {
                val currentUid = firebaseAuth.currentUser?.uid
                android.util.Log.d("MainActivity", "Auth state changed: uid=$currentUid, email=${firebaseAuth.currentUser?.email}")

                if (!authStateInitialized) {
                    authStateInitialized = true
                    observedAuthUid = currentUid
                    if (currentUid != null) {
                        android.util.Log.d("MainActivity", "First auth detected, starting sync lifecycle")
                        firestoreSyncService.stopRealtimeSync()
                        firestoreSyncService.pullFromCloud()
                        firestoreSyncService.pushAllToCloud()
                        firestoreSyncService.startRealtimeSync()
                    }
                    return@launch
                }

                if (currentUid == observedAuthUid) {
                    return@launch
                }

                firestoreSyncService.stopRealtimeSync()
                firestoreSyncService.clearLocalSyncData()

                if (currentUid != null) {
                    firestoreSyncService.pullFromCloud()
                    firestoreSyncService.pushAllToCloud()
                    firestoreSyncService.startRealtimeSync()
                }

                observedAuthUid = currentUid
            }
        }
        authStateListener?.let { auth.addAuthStateListener(it) }
    }

    override fun onDestroy() {
        if (BuildConfig.FEATURE_FIREBASE) {
            val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
            authStateListener?.let { listener -> auth?.removeAuthStateListener(listener) }
            firestoreSyncService.stopRealtimeSync()
        }
        super.onDestroy()
    }
}



@Composable
fun MediTrackApp(initialHighlightMedicineId: Int? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoutePattern = navBackStackEntry?.destination?.route

    val currentRoute = when {
        currentRoutePattern?.startsWith("home") == true -> "home"
        currentRoutePattern == Screen.History.route -> Screen.History.route
        currentRoutePattern == Screen.Report.route -> Screen.Report.route
        currentRoutePattern == Screen.Settings.route -> Screen.Settings.route
        else -> currentRoutePattern
    }

    val startDestination = if (BuildConfig.FEATURE_FIREBASE) {
        val hasUser = runCatching { FirebaseAuth.getInstance().currentUser != null }.getOrDefault(false)
        if (hasUser) Screen.Home.createRoute(initialHighlightMedicineId ?: -1) else Screen.Login.route
    } else {
        Screen.Home.createRoute(initialHighlightMedicineId ?: -1)
    }

    val showBottomBar = currentRoute in listOf(
        "home",
        Screen.History.route,
        Screen.Report.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        val targetRoute = when (screen) {
                            is Screen.Home -> Screen.Home.createRoute()
                            else -> screen.route
                        }
                        navController.navigate(targetRoute) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(padding),
            startDestination = startDestination
        )
    }
}
