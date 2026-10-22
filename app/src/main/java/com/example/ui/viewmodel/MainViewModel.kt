package com.example.ui.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.JourneyEntity
import com.example.data.local.PreferencesManager
import com.example.data.model.ActiveJourneyState
import com.example.data.model.AlarmConfiguration
import com.example.data.model.AppLanguage
import com.example.data.model.BangladeshTransitHubs
import com.example.data.model.Destination
import com.example.data.model.DestinationCategory
import com.example.data.model.JourneyStatus
import com.example.data.model.LocationReading
import com.example.data.repository.JourneyRepository
import com.example.engine.AlarmEngine
import com.example.engine.GeoUtils
import com.example.engine.GeocodingHelper
import com.example.engine.JourneySimulator
import com.example.service.LocationForegroundService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: JourneyRepository
    private val simulator = JourneySimulator()
    private val geocodingHelper = GeocodingHelper(application)
    private val osrmRoutingService = com.example.engine.OsrmRoutingService()
    private val updateManager = com.example.engine.UpdateManager(application)
    private var searchJob: Job? = null
    private var routeFetchJob: Job? = null
    private var downloadJob: Job? = null
    private val testAlarmEngine: AlarmEngine by lazy { AlarmEngine(application) }

    val journeyState: StateFlow<ActiveJourneyState> = LocationForegroundService.journeyState

    private val _updateStatus = MutableStateFlow<com.example.data.model.UpdateStatus>(com.example.data.model.UpdateStatus.Idle)
    val updateStatus: StateFlow<com.example.data.model.UpdateStatus> = _updateStatus.asStateFlow()

    private val _routeData = MutableStateFlow<com.example.data.model.RouteData?>(null)
    val routeData: StateFlow<com.example.data.model.RouteData?> = _routeData.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _selectedDestination = MutableStateFlow<Destination?>(null)
    val selectedDestination: StateFlow<Destination?> = _selectedDestination.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Destination>>(BangladeshTransitHubs.hubs)
    val searchResults: StateFlow<List<Destination>> = _searchResults.asStateFlow()

    private val _userLocation = MutableStateFlow<LocationReading?>(
        LocationReading(latitude = 23.7779, longitude = 90.3475, accuracyMeters = 10f)
    )
    val userLocation: StateFlow<LocationReading?> = _userLocation.asStateFlow()

    private val _showConfirmSheet = MutableStateFlow(false)
    val showConfirmSheet: StateFlow<Boolean> = _showConfirmSheet.asStateFlow()

    private val _isTestingAlarm = MutableStateFlow(false)
    val isTestingAlarm: StateFlow<Boolean> = _isTestingAlarm.asStateFlow()

    private val _systemSounds = MutableStateFlow<List<Pair<String, String>>>(
        listOf(
            Pair("Default Alarm", android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)?.toString() ?: ""),
            Pair("Default Ringtone", android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)?.toString() ?: ""),
            Pair("Default Notification", android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)?.toString() ?: "")
        )
    )
    val systemSounds: StateFlow<List<Pair<String, String>>> = _systemSounds.asStateFlow()

    val journeyHistory: StateFlow<List<JourneyEntity>>
    val recentJourneys: StateFlow<List<JourneyEntity>>
    val alarmConfig: StateFlow<AlarmConfiguration>
    val currentLanguage: StateFlow<AppLanguage>
    val isSimulatorEnabled: StateFlow<Boolean>
    val isOnboardingCompleted: StateFlow<Boolean>

    init {
        val db = AppDatabase.getDatabase(application)
        val prefs = PreferencesManager(application)
        repository = JourneyRepository(db.journeyDao(), prefs)

        journeyHistory = repository.allJourneys.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        recentJourneys = repository.recentJourneys.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        alarmConfig = repository.alarmConfig.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), AlarmConfiguration()
        )
        currentLanguage = repository.currentLanguage.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.BANGLA
        )
        isSimulatorEnabled = repository.isSimulatorEnabled.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), false
        )
        isOnboardingCompleted = repository.isOnboardingCompleted.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), true
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val manager = android.media.RingtoneManager(application)
                manager.setType(android.media.RingtoneManager.TYPE_ALL)
                val cursor = manager.cursor
                val list = mutableListOf<Pair<String, String>>()
                try {
                    while (cursor != null && cursor.moveToNext()) {
                        val title = cursor.getString(android.media.RingtoneManager.TITLE_COLUMN_INDEX)
                        val uri = manager.getRingtoneUri(cursor.position)
                        if (title != null && uri != null) {
                            list.add(Pair(title, uri.toString()))
                        }
                    }
                } finally {
                    try { cursor?.close() } catch (_: Exception) {}
                }
                if (list.isNotEmpty()) {
                    _systemSounds.value = list
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error pre-loading system sounds", e)
            }
        }

        observeNetworkConnectivity()
        checkForAppUpdate(checkedManually = false)

        viewModelScope.launch {
            simulator.simulatedLocation.collect { simLoc ->
                if (simLoc != null) {
                    _userLocation.value = simLoc
                    if (journeyState.value.status == JourneyStatus.TRACKING ||
                        journeyState.value.status == JourneyStatus.ALARM_TRIGGERED) {
                        val intent = Intent(application, LocationForegroundService::class.java).apply {
                            action = LocationForegroundService.ACTION_UPDATE_SIMULATED_LOCATION
                            putExtra(LocationForegroundService.EXTRA_START_LAT, simLoc.latitude)
                            putExtra(LocationForegroundService.EXTRA_START_LNG, simLoc.longitude)
                            putExtra("EXTRA_BEARING", simLoc.bearing)
                            putExtra("EXTRA_SPEED", simLoc.speedKmh)
                        }
                        application.startService(intent)
                    }
                }
            }
        }
    }

    private fun observeNetworkConnectivity() {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

        fun checkConnectivity(): Boolean {
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        _isNetworkAvailable.value = checkConnectivity()

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isNetworkAvailable.value = true
            }

            override fun onLost(network: Network) {
                _isNetworkAvailable.value = checkConnectivity()
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                _isNetworkAvailable.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }

        try {
            cm.registerNetworkCallback(request, callback)
        } catch (_: Exception) {}
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _searchResults.value = BangladeshTransitHubs.hubs
            return
        }

        val q = trimmed.lowercase()
        val localMatches = BangladeshTransitHubs.hubs.filter {
            it.nameBn.lowercase().contains(q) ||
            it.nameEn.lowercase().contains(q) ||
            it.addressBn.lowercase().contains(q) ||
            it.addressEn.lowercase().contains(q)
        }
        _searchResults.value = localMatches

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(250)
            val geocodedResults = geocodingHelper.searchPlaces(trimmed, maxResults = 8)
            if (geocodedResults.isNotEmpty() && _searchQuery.value == query) {
                val merged = (localMatches + geocodedResults).distinctBy {
                    "${String.format("%.3f", it.latitude)}_${String.format("%.3f", it.longitude)}"
                }
                _searchResults.value = merged
            }
        }
    }

    fun fetchRouteForDestination(dest: Destination) {
        val currentLoc = _userLocation.value ?: LocationReading(23.7779, 90.3475)
        routeFetchJob?.cancel()
        _routeData.value = com.example.data.model.RouteData(isLoading = true)
        routeFetchJob = viewModelScope.launch {
            val route = osrmRoutingService.fetchRoute(
                startLat = currentLoc.latitude,
                startLng = currentLoc.longitude,
                destLat = dest.latitude,
                destLng = dest.longitude
            )
            _routeData.value = route
        }
    }

    fun selectDestination(dest: Destination) {
        _selectedDestination.value = dest
        fetchRouteForDestination(dest)
    }

    fun deselectDestination() {
        routeFetchJob?.cancel()
        _selectedDestination.value = null
        _routeData.value = null
        _showConfirmSheet.value = false
    }

    fun openConfirmSheet() {
        if (_selectedDestination.value != null) {
            _showConfirmSheet.value = true
        }
    }

    fun selectDestinationFromCoordinates(lat: Double, lng: Double) {
        val tempDest = Destination(
            id = "custom_${System.currentTimeMillis()}",
            nameBn = "নির্বাচিত পয়েন্ট",
            nameEn = "Selected Point",
            addressBn = "অক্ষাংশ: ${String.format("%.4f", lat)}, দ্রাঘিমাংশ: ${String.format("%.4f", lng)}",
            addressEn = "Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)}",
            latitude = lat,
            longitude = lng,
            category = DestinationCategory.CUSTOM
        )
        _selectedDestination.value = tempDest
        fetchRouteForDestination(tempDest)

        viewModelScope.launch {
            val (placeName, fullAddress) = geocodingHelper.reverseGeocode(lat, lng)
            if (_selectedDestination.value?.latitude == lat && _selectedDestination.value?.longitude == lng) {
                _selectedDestination.value = tempDest.copy(
                    nameBn = placeName,
                    nameEn = placeName,
                    addressBn = fullAddress,
                    addressEn = fullAddress
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentDeviceLocation(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        updateUserLocation(
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

    fun dismissConfirmSheet() {
        _showConfirmSheet.value = false
    }

    fun updateUserLocation(reading: LocationReading) {
        if (!journeyState.value.isSimulated) {
            _userLocation.value = reading
        }
    }

    fun updateThreshold(meters: Int) {
        viewModelScope.launch {
            repository.updateThreshold(meters)
        }
    }

    fun updateAlarmConfig(config: AlarmConfiguration) {
        viewModelScope.launch {
            repository.updateAlarmConfig(config)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setLanguage(language)
            if (journeyState.value.status == JourneyStatus.TRACKING ||
                journeyState.value.status == JourneyStatus.ALARM_TRIGGERED ||
                journeyState.value.status == JourneyStatus.ALARM_SNOOZED) {
                val intent = Intent(getApplication(), LocationForegroundService::class.java).apply {
                    action = LocationForegroundService.ACTION_UPDATE_LANGUAGE
                    putExtra(LocationForegroundService.EXTRA_LANGUAGE, language.name)
                }
                getApplication<Application>().startService(intent)
            }
        }
    }

    fun setSimulatorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSimulatorEnabled(enabled)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted(true)
        }
    }

    fun startJourney(context: Context, useSimulator: Boolean) {
        val destination = _selectedDestination.value ?: return
        val currentLoc = _userLocation.value ?: LocationReading(23.7779, 90.3475)
        val config = alarmConfig.value

        _showConfirmSheet.value = false

        val intent = Intent(context, LocationForegroundService::class.java).apply {
            action = LocationForegroundService.ACTION_START
            putExtra(LocationForegroundService.EXTRA_DEST_NAME_BN, destination.nameBn)
            putExtra(LocationForegroundService.EXTRA_DEST_NAME_EN, destination.nameEn)
            putExtra(LocationForegroundService.EXTRA_DEST_ADDRESS_BN, destination.addressBn)
            putExtra(LocationForegroundService.EXTRA_DEST_ADDRESS_EN, destination.addressEn)
            putExtra(LocationForegroundService.EXTRA_DEST_LAT, destination.latitude)
            putExtra(LocationForegroundService.EXTRA_DEST_LNG, destination.longitude)
            putExtra(LocationForegroundService.EXTRA_THRESHOLD_METERS, config.thresholdMeters)
            putExtra(LocationForegroundService.EXTRA_SOUND_ENABLED, config.soundEnabled)
            putExtra(LocationForegroundService.EXTRA_VIBE_ENABLED, config.vibrationEnabled)
            putExtra(LocationForegroundService.EXTRA_VOICE_ENABLED, config.voiceEnabled)
            putExtra(LocationForegroundService.EXTRA_MAX_VOLUME, config.maxVolume)
            putExtra(LocationForegroundService.EXTRA_SELECTED_ALARM_SOUND, config.selectedAlarmSound)
            putExtra(LocationForegroundService.EXTRA_CUSTOM_ALARM_PATH, config.customAlarmPath)
            putExtra(LocationForegroundService.EXTRA_START_LAT, currentLoc.latitude)
            putExtra(LocationForegroundService.EXTRA_START_LNG, currentLoc.longitude)
            putExtra(LocationForegroundService.EXTRA_IS_SIMULATED, useSimulator)
            putExtra(LocationForegroundService.EXTRA_LANGUAGE, currentLanguage.value.name)
        }

        context.startForegroundService(intent)

        val currentRoute = _routeData.value
        val hasPoints = currentRoute != null && currentRoute.points.size >= 2

        if (hasPoints) {
            LocationForegroundService.setRouteData(currentRoute)
            if (useSimulator) {
                simulator.startSimulation(
                    scope = viewModelScope,
                    startLat = currentLoc.latitude,
                    startLng = currentLoc.longitude,
                    destination = destination,
                    routePoints = currentRoute!!.points,
                    speedMultiplier = 2.5
                )
            }
        } else {
            viewModelScope.launch {
                val route = osrmRoutingService.fetchRoute(
                    startLat = currentLoc.latitude,
                    startLng = currentLoc.longitude,
                    destLat = destination.latitude,
                    destLng = destination.longitude
                )
                _routeData.value = route
                LocationForegroundService.setRouteData(route)
                if (useSimulator) {
                    simulator.startSimulation(
                        scope = viewModelScope,
                        startLat = currentLoc.latitude,
                        startLng = currentLoc.longitude,
                        destination = destination,
                        routePoints = route.points,
                        speedMultiplier = 2.5
                    )
                }
            }
        }
    }

    fun stopJourney(context: Context) {
        simulator.stopSimulation()
        val current = journeyState.value

        val dest = current.destination
        if (dest != null && current.startTimeMillis > 0) {
            val journeyEntity = JourneyEntity(
                destinationNameBn = dest.nameBn,
                destinationNameEn = dest.nameEn,
                destinationAddress = dest.addressBn,
                destinationLat = dest.latitude,
                destinationLng = dest.longitude,
                startLat = current.startLocation?.latitude ?: 0.0,
                startLng = current.startLocation?.longitude ?: 0.0,
                thresholdMeters = current.config.thresholdMeters,
                totalDistanceMeters = current.initialTotalDistanceMeters,
                startTimeMillis = current.startTimeMillis,
                endTimeMillis = System.currentTimeMillis(),
                alarmTriggered = current.alarmTriggeredTimeMillis != null,
                wasSimulated = current.isSimulated
            )
            viewModelScope.launch {
                repository.saveCompletedJourney(journeyEntity)
            }
        }

        val intent = Intent(context, LocationForegroundService::class.java).apply {
            action = LocationForegroundService.ACTION_STOP
        }
        context.startService(intent)
        LocationForegroundService.resetState()
        _selectedDestination.value = null
    }

    fun dismissAlarm(context: Context) {
        val intent = Intent(context, LocationForegroundService::class.java).apply {
            action = LocationForegroundService.ACTION_DISMISS_ALARM
        }
        context.startService(intent)
    }

    fun snoozeAlarm(context: Context) {
        val intent = Intent(context, LocationForegroundService::class.java).apply {
            action = LocationForegroundService.ACTION_SNOOZE_ALARM
        }
        context.startService(intent)
    }

    fun jumpSimulatorNearThreshold() {
        val state = journeyState.value
        val dest = state.destination ?: return
        val start = state.startLocation ?: return
        val threshold = state.config.thresholdMeters
        simulator.jumpNearThreshold(
            startLat = start.latitude,
            startLng = start.longitude,
            destination = dest,
            targetDistanceMeters = (threshold - 50).toDouble()
        )
    }

    fun testEmergencyAlarm() {
        if (_isTestingAlarm.value) return
        _isTestingAlarm.value = true
        testAlarmEngine.startAlarm(
            scope = viewModelScope,
            config = alarmConfig.value,
            language = currentLanguage.value,
            destinationName = if (currentLanguage.value == AppLanguage.BANGLA) "টেস্ট গন্তব্য" else "Test Destination"
        )

        viewModelScope.launch {
            kotlinx.coroutines.delay(6000L)
            stopEmergencyAlarmTest()
        }
    }

    fun stopEmergencyAlarmTest() {
        testAlarmEngine.stopAlarm()
        _isTestingAlarm.value = false
    }

    fun deleteJourneyRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteJourney(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun checkForAppUpdate(checkedManually: Boolean = false) {
        viewModelScope.launch {
            if (checkedManually) {
                _updateStatus.value = com.example.data.model.UpdateStatus.Checking
            }
            val result = updateManager.checkForUpdate(checkedManually = checkedManually)
            _updateStatus.value = result
        }
    }

    fun startUpdateDownload(apkUrl: String) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            updateManager.downloadApk(apkUrl).collect { status ->
                _updateStatus.value = status
                if (status is com.example.data.model.UpdateStatus.DownloadCompleted) {
                    updateManager.installApk(status.apkFile)
                }
            }
        }
    }

    fun installDownloadedApk(apkFile: java.io.File) {
        updateManager.installApk(apkFile)
    }

    fun dismissUpdateDialog() {
        downloadJob?.cancel()
        _updateStatus.value = com.example.data.model.UpdateStatus.Idle
    }
}
