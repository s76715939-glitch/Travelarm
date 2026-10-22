package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppLanguage
import com.example.data.model.JourneyStatus
import com.example.data.model.LocationReading
import com.example.ui.components.ProfessionalBottomBar
import com.example.ui.screens.ActiveJourneyScreen
import com.example.ui.screens.AlarmScreen
import com.example.ui.screens.DestinationConfirmSheet
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WelcomeSplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.util.BatteryOptimizationHelper
import com.google.android.gms.location.LocationServices

enum class MainTab {
    MAP,
    HISTORY,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                window.decorView.display?.let { display ->
                    val modes = display.supportedModes
                    val maxMode = modes.maxByOrNull { it.refreshRate }
                    maxMode?.let {
                        val params = window.attributes
                        params.preferredDisplayModeId = it.modeId
                        window.attributes = params
                    }
                }
            } catch (_: Exception) {}
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun requestInitialLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(this)
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.updateUserLocation(
                        LocationReading(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = location.accuracy,
                            speedKmh = location.speed * 3.6f,
                            timestamp = location.time
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MainTab.MAP) }
    val snackbarHostState = remember { SnackbarHostState() }

    val journeyState by viewModel.journeyState.collectAsStateWithLifecycle()
    val selectedDestination by viewModel.selectedDestination.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val showConfirmSheet by viewModel.showConfirmSheet.collectAsStateWithLifecycle()
    val isTestingAlarm by viewModel.isTestingAlarm.collectAsStateWithLifecycle()
    val routeData by viewModel.routeData.collectAsStateWithLifecycle()

    val journeyHistory by viewModel.journeyHistory.collectAsStateWithLifecycle()
    val recentJourneys by viewModel.recentJourneys.collectAsStateWithLifecycle()
    val alarmConfig by viewModel.alarmConfig.collectAsStateWithLifecycle()
    val language by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsStateWithLifecycle()
    val systemSounds by viewModel.systemSounds.collectAsStateWithLifecycle()

    var isBatteryOptimizationIgnored by remember {
        mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }
    var userDismissedBatteryPopup by rememberSaveable { mutableStateOf(false) }
    var showWelcomeScreen by rememberSaveable { mutableStateOf(true) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryOptimizationIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocationGranted || coarseLocationGranted) {
            (context as? MainActivity)?.requestInitialLocation()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
        (context as? MainActivity)?.requestInitialLocation()
    }

    LaunchedEffect(journeyState.status) {
        val activity = context as? MainActivity
        if (activity != null) {
            val isAlarm = journeyState.status == JourneyStatus.ALARM_TRIGGERED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity.setShowWhenLocked(isAlarm)
                activity.setTurnScreenOn(isAlarm)
            } else {
                @Suppress("DEPRECATION")
                if (isAlarm) {
                    activity.window.addFlags(
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    )
                } else {
                    activity.window.clearFlags(
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    )
                }
            }
        }
    }

    // 1. High-priority Alarm Full-Screen Override
    if (journeyState.status == JourneyStatus.ALARM_TRIGGERED) {
        AlarmScreen(
            journeyState = journeyState,
            language = language,
            onStopJourney = { viewModel.stopJourney(context) },
            onSnooze = { viewModel.snoozeAlarm(context) }
        )
        return
    }

    // 2. Custom Welcome Splash Screen
    if (showWelcomeScreen && journeyState.status != JourneyStatus.TRACKING && journeyState.status != JourneyStatus.ALARM_SNOOZED) {
        WelcomeSplashScreen(
            onFinished = { showWelcomeScreen = false }
        )
        return
    }

    // 3. Main Scaffold Structure
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            when {
                journeyState.status == JourneyStatus.TRACKING || journeyState.status == JourneyStatus.ALARM_SNOOZED -> {
                    ActiveJourneyScreen(
                        journeyState = journeyState,
                        language = language,
                        onEndJourney = { viewModel.stopJourney(context) },
                        onFastForwardSimulator = { viewModel.jumpSimulatorNearThreshold() },
                        onDismissAlarm = { viewModel.dismissAlarm(context) }
                    )
                }

                currentTab == MainTab.MAP -> {
                    HomeScreen(
                        currentLocation = userLocation,
                        selectedDestination = selectedDestination,
                        thresholdMeters = alarmConfig.thresholdMeters,
                        searchQuery = searchQuery,
                        searchResults = searchResults,
                        recentJourneys = recentJourneys,
                        language = language,
                        routeData = routeData,
                        isNetworkAvailable = isNetworkAvailable,
                        showBatteryOptimizationPopup = !isBatteryOptimizationIgnored && !userDismissedBatteryPopup,
                        onDismissBatteryPopup = {
                            userDismissedBatteryPopup = true
                        },
                        onRequestBatteryOptimization = {
                            BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
                        },
                        onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                        onDestinationSelect = { viewModel.selectDestination(it) },
                        onDeselectDestination = { viewModel.deselectDestination() },
                        onMapTap = { lat, lng -> viewModel.selectDestinationFromCoordinates(lat, lng) },
                        onOpenConfirmSheet = {
                            viewModel.openConfirmSheet()
                        },
                        onRequestCurrentLocation = {
                            viewModel.fetchCurrentDeviceLocation(context)
                        }
                    )
                }

                currentTab == MainTab.HISTORY -> {
                    HistoryScreen(
                        journeys = journeyHistory,
                        language = language,
                        onDeleteJourney = { viewModel.deleteJourneyRecord(it) },
                        onClearAll = { viewModel.clearAllHistory() }
                    )
                }

                currentTab == MainTab.SETTINGS -> {
                    SettingsScreen(
                        language = language,
                        alarmConfig = alarmConfig,
                        isTestingAlarm = isTestingAlarm,
                        isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                        systemSounds = systemSounds,
                        onLanguageChange = { viewModel.setLanguage(it) },
                        onAlarmConfigChange = { viewModel.updateAlarmConfig(it) },
                        onTestAlarm = { viewModel.testEmergencyAlarm() },
                        onStopTestAlarm = { viewModel.stopEmergencyAlarmTest() },
                        onRequestBatteryOptimization = {
                            BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
                        }
                    )
                }
            }

            if (showConfirmSheet && selectedDestination != null) {
                DestinationConfirmSheet(
                    destination = selectedDestination!!,
                    currentLocation = userLocation,
                    alarmConfig = alarmConfig,
                    language = language,
                    onDismiss = { viewModel.dismissConfirmSheet() },
                    onThresholdChange = { viewModel.updateThreshold(it) },
                    onConfigChange = { viewModel.updateAlarmConfig(it) },
                    onStartJourney = { useSimulator ->
                        viewModel.startJourney(context, useSimulator)
                    }
                )
            }

            if (!isOnboardingCompleted) {
                OnboardingDialog(
                    language = language,
                    onConfirm = { viewModel.completeOnboarding() }
                )
            }

            if (journeyState.status != JourneyStatus.TRACKING && 
                journeyState.status != JourneyStatus.ALARM_TRIGGERED &&
                journeyState.status != JourneyStatus.ALARM_SNOOZED) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    ProfessionalBottomBar(
                        currentTab = currentTab,
                        language = language,
                        onTabSelected = { currentTab = it }
                    )
                }
            }
        }
    }
}
