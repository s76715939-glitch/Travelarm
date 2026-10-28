package com.sstechzone.travelarm.data.model

import org.osmdroid.util.GeoPoint

data class RouteStep(
    val id: Int,
    val instructionBn: String,
    val instructionEn: String,
    val streetName: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val maneuverType: String,
    val maneuverModifier: String?,
    val location: GeoPoint
)

data class RouteData(
    val points: List<GeoPoint> = emptyList(),
    val distanceMeters: Double = 0.0,
    val durationSeconds: Double = 0.0,
    val summary: String = "",
    val steps: List<RouteStep> = emptyList(),
    val isLoading: Boolean = false,
    val isFallback: Boolean = false,
    val errorMessage: String? = null
)
