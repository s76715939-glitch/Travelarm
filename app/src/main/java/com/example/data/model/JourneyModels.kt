package com.example.data.model

enum class JourneyStatus {
    IDLE,
    TRACKING,
    ALARM_TRIGGERED,
    ALARM_SNOOZED,
    FINISHED,
    CANCELLED
}

enum class GpsSignalQuality {
    EXCELLENT,
    GOOD,
    MODERATE,
    WEAK,
    LOST
}

data class LocationReading(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 10f,
    val speedKmh: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val isSimulated: Boolean = false,
    val bearing: Float = 0f
)

data class AlarmConfiguration(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val voiceEnabled: Boolean = true,
    val thresholdMeters: Int = 1000,
    val maxVolume: Boolean = true,
    val selectedAlarmSound: String = "DEFAULT_ALARM",
    val customAlarmPath: String = ""
)

data class ActiveJourneyState(
    val status: JourneyStatus = JourneyStatus.IDLE,
    val destination: Destination? = null,
    val startLocation: LocationReading? = null,
    val currentLocation: LocationReading? = null,
    val remainingDistanceMeters: Double = 0.0,
    val distanceToThresholdMeters: Double = 0.0,
    val initialTotalDistanceMeters: Double = 0.0,
    val estimatedArrivalSeconds: Long = 0,
    val startTimeMillis: Long = 0,
    val alarmTriggeredTimeMillis: Long? = null,
    val isSimulated: Boolean = false,
    val gpsQuality: GpsSignalQuality = GpsSignalQuality.GOOD,
    val config: AlarmConfiguration = AlarmConfiguration(),
    val isRouteRoadEstimated: Boolean = false,
    val routeData: RouteData? = null
)

enum class AppLanguage {
    BANGLA,
    ENGLISH
}
