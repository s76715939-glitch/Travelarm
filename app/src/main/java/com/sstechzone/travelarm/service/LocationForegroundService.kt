package com.sstechzone.travelarm.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sstechzone.travelarm.MainActivity
import com.sstechzone.travelarm.R
import com.sstechzone.travelarm.data.local.PreferencesManager
import com.sstechzone.travelarm.data.model.ActiveJourneyState
import com.sstechzone.travelarm.data.model.AlarmConfiguration
import com.sstechzone.travelarm.data.model.AppLanguage
import com.sstechzone.travelarm.data.model.Destination
import com.sstechzone.travelarm.data.model.GpsSignalQuality
import com.sstechzone.travelarm.data.model.JourneyStatus
import com.sstechzone.travelarm.data.model.LocationReading
import com.sstechzone.travelarm.engine.AlarmEngine
import com.sstechzone.travelarm.engine.GeoUtils
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var alarmEngine: AlarmEngine? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentLanguage: AppLanguage = AppLanguage.BANGLA

    // For instantaneous speed calculation across consecutive 500ms fixes
    private var lastLocation: Location? = null
    private var lastNotifUpdateTime: Long = 0L

    companion object {
        const val CHANNEL_ID = "travelarm_tracking_channel"
        const val ALARM_CHANNEL_ID = "travelarm_alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val ALARM_NOTIFICATION_ID = 1002

        const val EXTRA_LANGUAGE = "extra_language"
        const val ACTION_START = "com.sstechzone.travelarm.action.START_JOURNEY"
        const val ACTION_STOP = "com.sstechzone.travelarm.action.STOP_JOURNEY"
        const val ACTION_DISMISS_ALARM = "com.sstechzone.travelarm.action.DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.sstechzone.travelarm.action.SNOOZE_ALARM"
        const val ACTION_UPDATE_SIMULATED_LOCATION = "com.sstechzone.travelarm.action.UPDATE_SIMULATED_LOCATION"
        const val ACTION_UPDATE_LANGUAGE = "com.sstechzone.travelarm.action.UPDATE_LANGUAGE"

        const val EXTRA_DEST_NAME_BN = "dest_name_bn"
        const val EXTRA_DEST_NAME_EN = "dest_name_en"
        const val EXTRA_DEST_ADDRESS_BN = "dest_address_bn"
        const val EXTRA_DEST_ADDRESS_EN = "dest_address_en"
        const val EXTRA_DEST_LAT = "dest_lat"
        const val EXTRA_DEST_LNG = "dest_lng"
        const val EXTRA_THRESHOLD_METERS = "threshold_meters"
        const val EXTRA_SOUND_ENABLED = "sound_enabled"
        const val EXTRA_VIBE_ENABLED = "vibe_enabled"
        const val EXTRA_VOICE_ENABLED = "voice_enabled"
        const val EXTRA_MAX_VOLUME = "max_volume"
        const val EXTRA_SELECTED_ALARM_SOUND = "selected_alarm_sound"
        const val EXTRA_CUSTOM_ALARM_PATH = "custom_alarm_path"
        const val EXTRA_START_LAT = "start_lat"
        const val EXTRA_START_LNG = "start_lng"
        const val EXTRA_IS_SIMULATED = "is_simulated"

        // Observable state for Compose UI
        private val _journeyState = MutableStateFlow(ActiveJourneyState())
        val journeyState: StateFlow<ActiveJourneyState> = _journeyState.asStateFlow()

        fun updateState(transform: (ActiveJourneyState) -> ActiveJourneyState) {
            _journeyState.value = transform(_journeyState.value)
        }

        fun resetState() {
            _journeyState.value = ActiveJourneyState(status = JourneyStatus.IDLE)
        }

        fun setRouteData(route: com.sstechzone.travelarm.data.model.RouteData?) {
            _journeyState.value = _journeyState.value.copy(routeData = route)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        alarmEngine = AlarmEngine(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Travelarm:WakeLock")

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    handleNewLocation(location)
                }
            }
        }

        serviceScope.launch {
            try {
                PreferencesManager(applicationContext).alarmConfigFlow.collect { config ->
                    if (isActive) {
                        _journeyState.value = _journeyState.value.copy(config = config)
                    }
                }
            } catch (e: CancellationException) {
                // Normal job cancellation, ignore
            } catch (e: Exception) {
                Log.e("LocationService", "Error collecting alarm config flow", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                handleStartJourney(intent)
            }
            ACTION_STOP -> {
                handleStopJourney()
            }
            ACTION_DISMISS_ALARM -> {
                handleDismissAlarm()
            }
            ACTION_SNOOZE_ALARM -> {
                handleSnoozeAlarm()
            }
            ACTION_UPDATE_LANGUAGE -> {
                val langStr = intent.getStringExtra(EXTRA_LANGUAGE)
                if (langStr != null) {
                    currentLanguage = try {
                        AppLanguage.valueOf(langStr)
                    } catch (_: Exception) {
                        AppLanguage.BANGLA
                    }
                    val current = _journeyState.value
                    val dest = current.destination
                    if (dest != null && current.status == JourneyStatus.TRACKING) {
                        val name = if (currentLanguage == AppLanguage.ENGLISH) dest.nameEn else dest.nameBn
                        updateOngoingNotification(name, current.remainingDistanceMeters)
                    }
                }
            }
            ACTION_UPDATE_SIMULATED_LOCATION -> {
                val lat = intent.getDoubleExtra(EXTRA_START_LAT, 0.0)
                val lng = intent.getDoubleExtra(EXTRA_START_LNG, 0.0)
                val bearing = intent.getFloatExtra("EXTRA_BEARING", 0f)
                val speed = intent.getFloatExtra("EXTRA_SPEED", 45f)
                if (lat != 0.0 && lng != 0.0) {
                    val reading = LocationReading(
                        latitude = lat,
                        longitude = lng,
                        accuracyMeters = 5f,
                        speedKmh = speed,
                        timestamp = System.currentTimeMillis(),
                        isSimulated = true,
                        bearing = bearing
                    )
                    processLocationReading(reading)
                }
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("LocationService", "App swiped from Recents; foreground service continuing active tracking.")
    }

    private fun handleStartJourney(intent: Intent) {
        val langStr = intent.getStringExtra(EXTRA_LANGUAGE)
        currentLanguage = try {
            if (langStr != null) AppLanguage.valueOf(langStr) else AppLanguage.BANGLA
        } catch (_: Exception) {
            AppLanguage.BANGLA
        }

        val destNameBn = intent.getStringExtra(EXTRA_DEST_NAME_BN) ?: "গন্তব্য"
        val destNameEn = intent.getStringExtra(EXTRA_DEST_NAME_EN) ?: "Destination"
        val destAddressBn = intent.getStringExtra(EXTRA_DEST_ADDRESS_BN) ?: ""
        val destAddressEn = intent.getStringExtra(EXTRA_DEST_ADDRESS_EN) ?: ""
        val destLat = intent.getDoubleExtra(EXTRA_DEST_LAT, 0.0)
        val destLng = intent.getDoubleExtra(EXTRA_DEST_LNG, 0.0)
        val threshold = intent.getIntExtra(EXTRA_THRESHOLD_METERS, 1000)
        val sound = intent.getBooleanExtra(EXTRA_SOUND_ENABLED, true)
        val vibe = intent.getBooleanExtra(EXTRA_VIBE_ENABLED, true)
        val voice = intent.getBooleanExtra(EXTRA_VOICE_ENABLED, true)
        val maxVol = intent.getBooleanExtra(EXTRA_MAX_VOLUME, true)
        val selectedSound = intent.getStringExtra(EXTRA_SELECTED_ALARM_SOUND) ?: "DEFAULT_ALARM"
        val customPath = intent.getStringExtra(EXTRA_CUSTOM_ALARM_PATH) ?: ""
        val startLat = intent.getDoubleExtra(EXTRA_START_LAT, 0.0)
        val startLng = intent.getDoubleExtra(EXTRA_START_LNG, 0.0)
        val isSimulated = intent.getBooleanExtra(EXTRA_IS_SIMULATED, false)

        val destination = Destination(
            id = "active_dest",
            nameBn = destNameBn,
            nameEn = destNameEn,
            addressBn = destAddressBn,
            addressEn = destAddressEn,
            latitude = destLat,
            longitude = destLng
        )

        val config = AlarmConfiguration(
            soundEnabled = sound,
            vibrationEnabled = vibe,
            voiceEnabled = voice,
            thresholdMeters = threshold,
            maxVolume = maxVol,
            selectedAlarmSound = selectedSound,
            customAlarmPath = customPath
        )

        val startReading = if (startLat != 0.0 && startLng != 0.0) {
            LocationReading(latitude = startLat, longitude = startLng, isSimulated = isSimulated)
        } else null

        val existingRoute = _journeyState.value.routeData
        val initialDistance = if (existingRoute != null && existingRoute.distanceMeters > 0) {
            existingRoute.distanceMeters
        } else if (startReading != null) {
            GeoUtils.calculateGeodesicDistanceMeters(startLat, startLng, destLat, destLng)
        } else 0.0

        val initialEta = if (existingRoute != null && existingRoute.durationSeconds > 0) {
            existingRoute.durationSeconds.toLong()
        } else {
            GeoUtils.estimateDurationSeconds(initialDistance, 35f)
        }

        _journeyState.value = ActiveJourneyState(
            status = JourneyStatus.TRACKING,
            destination = destination,
            startLocation = startReading,
            currentLocation = startReading,
            remainingDistanceMeters = initialDistance,
            distanceToThresholdMeters = (initialDistance - threshold).coerceAtLeast(0.0),
            initialTotalDistanceMeters = initialDistance,
            estimatedArrivalSeconds = initialEta,
            startTimeMillis = System.currentTimeMillis(),
            isSimulated = isSimulated,
            config = config,
            routeData = existingRoute
        )

        // Automatically fetch OSRM road polyline if missing or not yet loaded
        if (existingRoute == null || existingRoute.points.isEmpty()) {
            serviceScope.launch {
                try {
                    val route = com.sstechzone.travelarm.engine.OsrmRoutingService().fetchRoute(
                        startLat = startLat,
                        startLng = startLng,
                        destLat = destLat,
                        destLng = destLng
                    )
                    if (route.points.isNotEmpty()) {
                        val routeDist = if (route.distanceMeters > 0) route.distanceMeters else _journeyState.value.remainingDistanceMeters
                        val routeEta = if (route.durationSeconds > 0) route.durationSeconds.toLong() else _journeyState.value.estimatedArrivalSeconds
                        _journeyState.value = _journeyState.value.copy(
                            routeData = route,
                            remainingDistanceMeters = routeDist,
                            initialTotalDistanceMeters = routeDist,
                            distanceToThresholdMeters = (routeDist - threshold).coerceAtLeast(0.0),
                            estimatedArrivalSeconds = routeEta
                        )
                    }
                } catch (e: Exception) {
                    Log.e("LocationService", "Error auto-fetching route", e)
                }
            }
        }

        val displayName = if (currentLanguage == AppLanguage.ENGLISH) destination.nameEn else destination.nameBn
        val notification = buildOngoingNotification(displayName, initialDistance)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (!isSimulated) {
            startLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100L)
            .setMinUpdateIntervalMillis(50L)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e("LocationService", "Error requesting location updates", e)
        }
    }

    private fun handleNewLocation(location: Location) {
        var speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
        val prev = lastLocation
        var bearing = if (location.hasBearing() && location.bearing != 0f) location.bearing else 0f
        if (prev != null && location.time > prev.time) {
            val dtSeconds = (location.time - prev.time) / 1000.0
            if (dtSeconds in 0.3..6.0) {
                val distMeters = GeoUtils.calculateGeodesicDistanceMeters(
                    prev.latitude, prev.longitude,
                    location.latitude, location.longitude
                )
                val calculatedSpeed = ((distMeters / dtSeconds) * 3.6).toFloat()
                if (calculatedSpeed in 0.8f..150f) {
                    speedKmh = if (speedKmh > 0) (speedKmh * 0.4f + calculatedSpeed * 0.6f) else calculatedSpeed
                }
                if (distMeters > 1.5) {
                    bearing = GeoUtils.calculateBearing(prev.latitude, prev.longitude, location.latitude, location.longitude)
                }
            }
        }
        lastLocation = location

        val reading = LocationReading(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            speedKmh = speedKmh,
            timestamp = location.time,
            isSimulated = false,
            bearing = bearing
        )

        if (!GeoUtils.isValidGpsReading(reading)) {
            _journeyState.value = _journeyState.value.copy(gpsQuality = GpsSignalQuality.WEAK)
            return
        }

        processLocationReading(reading)
    }

    fun processLocationReading(reading: LocationReading) {
        val currentState = _journeyState.value
        val destination = currentState.destination ?: return

        val route = currentState.routeData
        val distance = if (route != null && route.points.size >= 2) {
            GeoUtils.calculateRemainingRouteDistanceMeters(
                reading.latitude,
                reading.longitude,
                route.points
            )
        } else {
            GeoUtils.calculateGeodesicDistanceMeters(
                reading.latitude,
                reading.longitude,
                destination.latitude,
                destination.longitude
            )
        }

        val threshold = currentState.config.thresholdMeters
        val distanceToThreshold = (distance - threshold).coerceAtLeast(0.0)

        val etaSeconds = if (route != null && route.distanceMeters > 0 && route.durationSeconds > 0) {
            val ratio = (distance / route.distanceMeters).coerceIn(0.0, 1.0)
            val baseDuration = (ratio * route.durationSeconds).toLong()
            if (reading.speedKmh > 15f) {
                val speedDuration = GeoUtils.estimateDurationSeconds(distance, reading.speedKmh)
                ((speedDuration * 0.6) + (baseDuration * 0.4)).toLong()
            } else {
                baseDuration
            }
        } else {
            val effectiveSpeed = if (reading.speedKmh > 5f) reading.speedKmh else 35f
            GeoUtils.estimateDurationSeconds(distance, effectiveSpeed)
        }

        val isCrossed = GeoUtils.isThresholdCrossed(
            currentDistanceMeters = distance,
            thresholdMeters = threshold,
            isAlreadyTriggered = currentState.status == JourneyStatus.ALARM_TRIGGERED
        )

        val gpsQuality = when {
            reading.accuracyMeters <= 15f -> GpsSignalQuality.EXCELLENT
            reading.accuracyMeters <= 35f -> GpsSignalQuality.GOOD
            reading.accuracyMeters <= 65f -> GpsSignalQuality.MODERATE
            else -> GpsSignalQuality.WEAK
        }

        _journeyState.value = currentState.copy(
            currentLocation = reading,
            remainingDistanceMeters = distance,
            distanceToThresholdMeters = distanceToThreshold,
            estimatedArrivalSeconds = etaSeconds,
            gpsQuality = gpsQuality
        )

        val now = System.currentTimeMillis()
        if (now - lastNotifUpdateTime >= 2500L || distanceToThreshold < 200.0) {
            lastNotifUpdateTime = now
            val destName = if (currentLanguage == AppLanguage.ENGLISH) destination.nameEn else destination.nameBn
            updateOngoingNotification(destName, distance)
        }

        if (isCrossed && currentState.status == JourneyStatus.TRACKING) {
            val destName = if (currentLanguage == AppLanguage.ENGLISH) destination.nameEn else destination.nameBn
            triggerAlarm(destName)
        }
    }

    private fun triggerAlarm(destName: String) {
        _journeyState.value = _journeyState.value.copy(
            status = JourneyStatus.ALARM_TRIGGERED,
            alarmTriggeredTimeMillis = System.currentTimeMillis()
        )

        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(10 * 60 * 1000L /* 10 minutes */)
            }
        } catch (e: Exception) {
            Log.e("LocationService", "Error acquiring wakelock", e)
        }

        alarmEngine?.startAlarm(
            scope = serviceScope,
            config = _journeyState.value.config,
            language = currentLanguage,
            destinationName = destName
        )

        showAlarmNotification(destName)
    }

    private fun handleDismissAlarm() {
        alarmEngine?.stopAlarm()
        releaseWakeLock()
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notifManager?.cancel(ALARM_NOTIFICATION_ID)

        _journeyState.value = _journeyState.value.copy(status = JourneyStatus.TRACKING)
    }

    private fun handleSnoozeAlarm() {
        alarmEngine?.stopAlarm()
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notifManager?.cancel(ALARM_NOTIFICATION_ID)

        _journeyState.value = _journeyState.value.copy(status = JourneyStatus.ALARM_SNOOZED)

        serviceScope.launch {
            kotlinx.coroutines.delay(3 * 60 * 1000L)
            if (_journeyState.value.status == JourneyStatus.ALARM_SNOOZED) {
                val dest = if (currentLanguage == AppLanguage.ENGLISH) {
                    _journeyState.value.destination?.nameEn ?: "Destination"
                } else {
                    _journeyState.value.destination?.nameBn ?: "গন্তব্য"
                }
                triggerAlarm(dest)
            }
        }
    }

    private fun handleStopJourney() {
        alarmEngine?.stopAlarm()
        releaseWakeLock()
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notifManager?.cancel(NOTIFICATION_ID)
        notifManager?.cancel(ALARM_NOTIFICATION_ID)

        _journeyState.value = _journeyState.value.copy(status = JourneyStatus.FINISHED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("LocationService", "Error releasing wakelock", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val trackingChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.alarm_channel_desc)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(trackingChannel)
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    private fun buildOngoingNotification(destName: String, distanceMeters: Double): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, LocationForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distText = GeoUtils.formatDistance(distanceMeters, currentLanguage)

        val title = if (currentLanguage == AppLanguage.ENGLISH) "Journey in progress: $destName" else "যাত্রা চলমান: $destName"
        val content = if (currentLanguage == AppLanguage.ENGLISH) "Remaining: $distText | Travelarm is monitoring" else "বাকি দূরত্ব: $distText | ট্রাভেলআর্ম নজর রাখছে"
        val actionText = if (currentLanguage == AppLanguage.ENGLISH) "End Journey" else "যাত্রা সমাপ্ত করুন"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .addAction(0, actionText, stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateOngoingNotification(destName: String, distanceMeters: Double) {
        val notification = buildOngoingNotification(destName, distanceMeters)
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notifManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun showAlarmNotification(destName: String) {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, LocationForegroundService::class.java).apply { action = ACTION_DISMISS_ALARM },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (currentLanguage == AppLanguage.ENGLISH) "ALERT! Destination Nearby!" else "সতর্কতা! গন্তব্য কাছাকাছি!"
        val content = if (currentLanguage == AppLanguage.ENGLISH) "Approaching $destName! Please wake up now." else "$destName স্টপেজ পৌঁছে গেছেন! ঘুম থেকে উঠুন।"
        val actionText = if (currentLanguage == AppLanguage.ENGLISH) "I'm Awake" else "আমি জেগে উঠেছি"

        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(0, actionText, dismissIntent)
            .build()

        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notifManager?.notify(ALARM_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        alarmEngine?.release()
        alarmEngine = null
        releaseWakeLock()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
