package com.example.engine

import com.example.data.model.Destination
import com.example.data.model.LocationReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class JourneySimulator {

    private val _simulatedLocation = MutableStateFlow<LocationReading?>(null)
    val simulatedLocation: StateFlow<LocationReading?> = _simulatedLocation.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var simulationJob: Job? = null
    private var progressRatio = 0.0 // 0.0 (start) to 1.0 (destination reached)
    private var activeRoutePoints: List<org.osmdroid.util.GeoPoint> = emptyList()

    /**
     * Starts or restarts simulated transit movement towards the destination.
     * If routePoints are provided (from OSRM API), the simulator smoothly drives along
     * the actual road turns and intersections.
     */
    fun startSimulation(
        scope: CoroutineScope,
        startLat: Double,
        startLng: Double,
        destination: Destination,
        routePoints: List<org.osmdroid.util.GeoPoint> = emptyList(),
        initialProgress: Double = 0.0,
        speedMultiplier: Double = 1.0
    ) {
        simulationJob?.cancel()
        progressRatio = initialProgress
        _isRunning.value = true
        activeRoutePoints = routePoints

        simulationJob = scope.launch(Dispatchers.Default) {
            val totalDistance = if (routePoints.size >= 2) {
                var sum = 0.0
                for (i in 0 until routePoints.size - 1) {
                    sum += GeoUtils.calculateGeodesicDistanceMeters(
                        routePoints[i].latitude, routePoints[i].longitude,
                        routePoints[i + 1].latitude, routePoints[i + 1].longitude
                    )
                }
                sum
            } else {
                GeoUtils.calculateGeodesicDistanceMeters(
                    startLat, startLng, destination.latitude, destination.longitude
                )
            }

            // Step configuration: simulate moving with ultra-realtime 100ms updates
            val stepTimeMillis = 100L
            val simulatedMps = 16.0 * speedMultiplier
            val stepFraction = if (totalDistance > 0) (simulatedMps * (stepTimeMillis / 1000.0)) / totalDistance else 0.05

            while (isActive && progressRatio < 1.0) {
                progressRatio = (progressRatio + stepFraction).coerceAtMost(1.0)

                val (currentLat, currentLng, segBearing) = if (routePoints.size >= 2) {
                    interpolateAlongPolyline(routePoints, progressRatio)
                } else {
                    val lat = startLat + (destination.latitude - startLat) * progressRatio
                    val lng = startLng + (destination.longitude - startLng) * progressRatio
                    val b = GeoUtils.calculateBearing(startLat, startLng, destination.latitude, destination.longitude)
                    Triple(lat, lng, b)
                }

                val simulatedReading = LocationReading(
                    latitude = currentLat,
                    longitude = currentLng,
                    accuracyMeters = 5.0f,
                    speedKmh = (simulatedMps * 3.6).toFloat(),
                    timestamp = System.currentTimeMillis(),
                    isSimulated = true,
                    bearing = segBearing
                )

                _simulatedLocation.value = simulatedReading

                if (progressRatio >= 1.0) {
                    break
                }

                delay(stepTimeMillis)
            }
            _isRunning.value = false
        }
    }

    fun updateRoutePoints(points: List<org.osmdroid.util.GeoPoint>) {
        if (points.size >= 2) {
            activeRoutePoints = points
        }
    }

    private fun interpolateAlongPolyline(
        points: List<org.osmdroid.util.GeoPoint>,
        fraction: Double
    ): Triple<Double, Double, Float> {
        if (points.isEmpty()) return Triple(0.0, 0.0, 0f)
        if (points.size == 1 || fraction <= 0.0) {
            val b = if (points.size >= 2) GeoUtils.calculateBearing(points[0].latitude, points[0].longitude, points[1].latitude, points[1].longitude) else 0f
            return Triple(points.first().latitude, points.first().longitude, b)
        }
        if (fraction >= 1.0) {
            val last = points.last()
            val prev = points[points.size - 2]
            val b = GeoUtils.calculateBearing(prev.latitude, prev.longitude, last.latitude, last.longitude)
            return Triple(last.latitude, last.longitude, b)
        }

        // Compute segment lengths
        val segmentDistances = DoubleArray(points.size - 1)
        var totalDist = 0.0
        for (i in 0 until points.size - 1) {
            val d = GeoUtils.calculateGeodesicDistanceMeters(
                points[i].latitude, points[i].longitude,
                points[i + 1].latitude, points[i + 1].longitude
            )
            segmentDistances[i] = d
            totalDist += d
        }

        if (totalDist == 0.0) return Triple(points.first().latitude, points.first().longitude, 0f)

        val targetDist = totalDist * fraction
        var accumulated = 0.0

        for (i in segmentDistances.indices) {
            val segLen = segmentDistances[i]
            if (accumulated + segLen >= targetDist || i == segmentDistances.lastIndex) {
                val segFraction = if (segLen > 0) ((targetDist - accumulated) / segLen).coerceIn(0.0, 1.0) else 0.0
                val p1 = points[i]
                val p2 = points[i + 1]
                val lat = p1.latitude + (p2.latitude - p1.latitude) * segFraction
                val lng = p1.longitude + (p2.longitude - p1.longitude) * segFraction
                val segBearing = GeoUtils.calculateBearing(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
                return Triple(lat, lng, segBearing)
            }
            accumulated += segLen
        }

        return Triple(points.last().latitude, points.last().longitude, 0f)
    }

    /**
     * Fast-forward simulation to jump directly near the threshold (for immediate testing).
     */
    fun jumpNearThreshold(
        startLat: Double,
        startLng: Double,
        destination: Destination,
        targetDistanceMeters: Double
    ) {
        val totalDistance = GeoUtils.calculateGeodesicDistanceMeters(
            startLat, startLng, destination.latitude, destination.longitude
        )
        val desiredRemaining = targetDistanceMeters.coerceAtLeast(10.0)
        progressRatio = if (totalDistance > desiredRemaining) {
            (totalDistance - desiredRemaining) / totalDistance
        } else {
            0.92
        }

        val (currentLat, currentLng, segBearing) = if (activeRoutePoints.size >= 2) {
            interpolateAlongPolyline(activeRoutePoints, progressRatio)
        } else {
            val lat = startLat + (destination.latitude - startLat) * progressRatio
            val lng = startLng + (destination.longitude - startLng) * progressRatio
            val b = GeoUtils.calculateBearing(startLat, startLng, destination.latitude, destination.longitude)
            Triple(lat, lng, b)
        }

        _simulatedLocation.value = LocationReading(
            latitude = currentLat,
            longitude = currentLng,
            accuracyMeters = 5.0f,
            speedKmh = 45f,
            timestamp = System.currentTimeMillis(),
            isSimulated = true,
            bearing = segBearing
        )
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        _isRunning.value = false
        _simulatedLocation.value = null
        progressRatio = 0.0
        activeRoutePoints = emptyList()
    }
}
