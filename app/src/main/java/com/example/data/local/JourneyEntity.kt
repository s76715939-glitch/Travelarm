package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journey_history")
data class JourneyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val destinationNameBn: String,
    val destinationNameEn: String,
    val destinationAddress: String,
    val destinationLat: Double,
    val destinationLng: Double,
    val startLat: Double,
    val startLng: Double,
    val thresholdMeters: Int,
    val totalDistanceMeters: Double,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val alarmTriggered: Boolean,
    val wasSimulated: Boolean
)
