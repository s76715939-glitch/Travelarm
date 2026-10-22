package com.example.engine

import com.example.data.model.AppLanguage
import com.example.data.model.LocationReading
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the great-circle geodesic distance between two points in meters using the Haversine formula.
     */
    fun calculateGeodesicDistanceMeters(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Double {
        val dLat = Math.toRadians(endLat - startLat)
        val dLng = Math.toRadians(endLng - startLng)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(startLat)) * cos(Math.toRadians(endLat)) *
                sin(dLng / 2) * sin(dLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calculates the remaining distance in meters along the polyline from current position to destination.
     * Finds the closest segment along the route and sums remaining segment distances.
     */
    fun calculateRemainingRouteDistanceMeters(
        currentLat: Double,
        currentLng: Double,
        routePoints: List<org.osmdroid.util.GeoPoint>
    ): Double {
        if (routePoints.isEmpty()) return 0.0
        if (routePoints.size == 1) {
            return calculateGeodesicDistanceMeters(currentLat, currentLng, routePoints[0].latitude, routePoints[0].longitude)
        }

        var minDistanceToSegment = Double.MAX_VALUE
        var closestSegmentIndex = 0
        var closestPointOnSegmentLat = routePoints[0].latitude
        var closestPointOnSegmentLng = routePoints[0].longitude

        for (i in 0 until routePoints.size - 1) {
            val p1 = routePoints[i]
            val p2 = routePoints[i + 1]

            val dx = p2.longitude - p1.longitude
            val dy = p2.latitude - p1.latitude
            val lenSq = dx * dx + dy * dy

            val (projLat, projLng) = if (lenSq > 1e-12) {
                val t = (((currentLng - p1.longitude) * dx + (currentLat - p1.latitude) * dy) / lenSq)
                    .coerceIn(0.0, 1.0)
                Pair(p1.latitude + t * dy, p1.longitude + t * dx)
            } else {
                Pair(p1.latitude, p1.longitude)
            }

            val dist = calculateGeodesicDistanceMeters(currentLat, currentLng, projLat, projLng)
            if (dist < minDistanceToSegment) {
                minDistanceToSegment = dist
                closestSegmentIndex = i
                closestPointOnSegmentLat = projLat
                closestPointOnSegmentLng = projLng
            }
        }

        // Distance from projected point to end of this segment
        val nextPoint = routePoints[closestSegmentIndex + 1]
        var remainingDistance = calculateGeodesicDistanceMeters(
            closestPointOnSegmentLat, closestPointOnSegmentLng,
            nextPoint.latitude, nextPoint.longitude
        )

        // Sum remaining segments to destination
        for (i in (closestSegmentIndex + 1) until (routePoints.size - 1)) {
            remainingDistance += calculateGeodesicDistanceMeters(
                routePoints[i].latitude, routePoints[i].longitude,
                routePoints[i + 1].latitude, routePoints[i + 1].longitude
            )
        }

        return remainingDistance
    }

    /**
     * Calculates initial bearing from start to destination in degrees (0..360).
     */
    fun calculateBearing(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Float {
        val phi1 = Math.toRadians(startLat)
        val phi2 = Math.toRadians(endLat)
        val deltaLambda = Math.toRadians(endLng - startLng)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

        val bearingRad = atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)
        return ((bearingDeg + 360) % 360).toFloat()
    }

    /**
     * Calculates bearing strictly along the route polyline or towards destination,
     * independent of device compass or orientation sensors.
     */
    fun calculateRouteBearing(
        currentLat: Double,
        currentLng: Double,
        routePoints: List<org.osmdroid.util.GeoPoint>?,
        destLat: Double?,
        destLng: Double?,
        currentBearing: Float = 0f
    ): Float {
        if (currentBearing > 0f) return currentBearing
        if (!routePoints.isNullOrEmpty() && routePoints.size >= 2) {
            var minDistance = Double.MAX_VALUE
            var closestIdx = 0
            for (i in routePoints.indices) {
                val p = routePoints[i]
                val dist = calculateGeodesicDistanceMeters(currentLat, currentLng, p.latitude, p.longitude)
                if (dist < minDistance) {
                    minDistance = dist
                    closestIdx = i
                }
            }
            // Find a target point ahead of closestIdx that is at least 8 meters away
            var targetIdx = closestIdx + 1
            while (targetIdx < routePoints.size) {
                val tp = routePoints[targetIdx]
                val d = calculateGeodesicDistanceMeters(currentLat, currentLng, tp.latitude, tp.longitude)
                if (d >= 8.0) {
                    break
                }
                targetIdx++
            }
            if (targetIdx >= routePoints.size) {
                targetIdx = routePoints.size - 1
            }
            val targetPoint = routePoints[targetIdx]
            val b = calculateBearing(currentLat, currentLng, targetPoint.latitude, targetPoint.longitude)
            if (b != 0f) return b
        }
        if (destLat != null && destLng != null) {
            return calculateBearing(currentLat, currentLng, destLat, destLng)
        }
        return 0f
    }

    /**
     * Filters noisy or stale GPS readings.
     * Rejects readings with accuracy worse than 65 meters or older than 30 seconds.
     */
    fun isValidGpsReading(
        reading: LocationReading,
        maxAccuracyMeters: Float = 65f,
        maxAgeMillis: Long = 30000L
    ): Boolean {
        if (reading.isSimulated) return true
        val isAccurate = reading.accuracyMeters in 0.01f..maxAccuracyMeters
        val isFresh = (System.currentTimeMillis() - reading.timestamp) <= maxAgeMillis
        return isAccurate && isFresh
    }

    /**
     * Checks if current distance has crossed into the alert threshold.
     * Incorporates hysteresis: once triggered, stays triggered until distance is threshold + 100m.
     */
    fun isThresholdCrossed(
        currentDistanceMeters: Double,
        thresholdMeters: Int,
        isAlreadyTriggered: Boolean,
        hysteresisBufferMeters: Double = 80.0
    ): Boolean {
        return if (!isAlreadyTriggered) {
            currentDistanceMeters <= thresholdMeters
        } else {
            // Keep triggered unless user moves significantly away past hysteresis boundary
            currentDistanceMeters <= (thresholdMeters + hysteresisBufferMeters)
        }
    }

    /**
     * Formats distance cleanly in Bangla or English numerals and units.
     */
    fun formatDistance(distanceMeters: Double, language: AppLanguage = AppLanguage.BANGLA): String {
        return if (distanceMeters >= 1000) {
            val km = distanceMeters / 1000.0
            val formatted = String.format("%.1f", km)
            if (language == AppLanguage.BANGLA) {
                "${toBengaliDigits(formatted)} কিমি"
            } else {
                "$formatted km"
            }
        } else {
            val m = distanceMeters.roundToInt().coerceAtLeast(0)
            if (language == AppLanguage.BANGLA) {
                "${toBengaliDigits(m.toString())} মি"
            } else {
                "$m m"
            }
        }
    }

    /**
     * Estimates remaining transit duration based on remaining distance and current transit speed.
     * Uses a realistic average public transit speed (35 km/h) if current speed is negligible.
     */
    fun estimateDurationSeconds(remainingDistanceMeters: Double, currentSpeedKmh: Float): Long {
        val effectiveSpeedKmh = if (currentSpeedKmh > 10f) currentSpeedKmh else 35f
        val speedMps = (effectiveSpeedKmh * 1000) / 3600.0
        return if (speedMps > 0) (remainingDistanceMeters / speedMps).toLong() else 0L
    }

    /**
     * Formats duration in hours and minutes in Bangla or English.
     */
    fun formatDuration(durationSeconds: Long, language: AppLanguage = AppLanguage.BANGLA): String {
        val totalMinutes = (durationSeconds / 60).coerceAtLeast(1)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60

        return if (language == AppLanguage.BANGLA) {
            if (hours > 0) {
                "${toBengaliDigits(hours.toString())} ঘণ্টা ${toBengaliDigits(mins.toString())} মিনিট"
            } else {
                "${toBengaliDigits(mins.toString())} মিনিট"
            }
        } else {
            if (hours > 0) {
                "$hours hr $mins min"
            } else {
                "$mins min"
            }
        }
    }

    /**
     * Projects the current location onto the nearest route segment and returns
     * a trimmed list of points starting from that projected point.
     */
    fun getInterpolatedRoute(
        currentLoc: org.osmdroid.util.GeoPoint,
        routePoints: List<org.osmdroid.util.GeoPoint>
    ): List<org.osmdroid.util.GeoPoint> {
        if (routePoints.size < 2) return routePoints

        var minDistanceToSegment = Double.MAX_VALUE
        var closestSegmentIndex = 0
        var projectedPoint = routePoints[0]

        for (i in 0 until routePoints.size - 1) {
            val p1 = routePoints[i]
            val p2 = routePoints[i + 1]

            val dx = p2.longitude - p1.longitude
            val dy = p2.latitude - p1.latitude
            val lenSq = dx * dx + dy * dy

            val proj = if (lenSq > 1e-12) {
                val t = (((currentLoc.longitude - p1.longitude) * dx + (currentLoc.latitude - p1.latitude) * dy) / lenSq)
                    .coerceIn(0.0, 1.0)
                org.osmdroid.util.GeoPoint(p1.latitude + t * dy, p1.longitude + t * dx)
            } else {
                p1
            }

            val dist = calculateGeodesicDistanceMeters(currentLoc.latitude, currentLoc.longitude, proj.latitude, proj.longitude)
            if (dist < minDistanceToSegment) {
                minDistanceToSegment = dist
                closestSegmentIndex = i
                projectedPoint = proj
            }
        }

        val result = mutableListOf<org.osmdroid.util.GeoPoint>()
        result.add(projectedPoint)
        for (i in (closestSegmentIndex + 1) until routePoints.size) {
            result.add(routePoints[i])
        }
        return result
    }

    /**
     * Converts ASCII English digits to Bengali script digits.
     */
    fun toBengaliDigits(input: String): String {

        val enDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        var result = input
        for (i in enDigits.indices) {
            result = result.replace(enDigits[i], bnDigits[i])
        }
        return result
    }
}
