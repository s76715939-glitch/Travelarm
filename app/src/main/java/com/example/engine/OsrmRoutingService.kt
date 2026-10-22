package com.example.engine

import android.util.Log
import com.example.data.model.RouteData
import com.example.data.model.RouteStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.util.Locale
import java.util.concurrent.TimeUnit

class OsrmRoutingService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Fetches driving route from OSRM API between start and destination coordinates.
     * Returns RouteData with full road polyline GeoPoints, turn-by-turn steps, distance & ETA.
     */
    suspend fun fetchRoute(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double
    ): RouteData = withContext(Dispatchers.IO) {
        // OSRM expects coordinates in {longitude},{latitude};{longitude},{latitude} format
        val url = String.format(
            Locale.US,
            "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson&steps=true",
            startLng, startLat, destLng, destLat
        )

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Travelarm/1.0 (Android App)")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                Log.w("OsrmRoutingService", "OSRM request failed with code: ${response.code}, falling back to direct line")
                return@withContext createFallbackRoute(startLat, startLng, destLat, destLng, "HTTP ${response.code}")
            }

            val json = JSONObject(responseBody)
            val code = json.optString("code")
            if (code != "Ok") {
                Log.w("OsrmRoutingService", "OSRM returned code: $code, using fallback")
                return@withContext createFallbackRoute(startLat, startLng, destLat, destLng, code)
            }

            val routesArray = json.optJSONArray("routes")
            if (routesArray == null || routesArray.length() == 0) {
                return@withContext createFallbackRoute(startLat, startLng, destLat, destLng, "No route found")
            }

            val primaryRoute = routesArray.getJSONObject(0)
            val totalDistance = primaryRoute.optDouble("distance", 0.0)
            val totalDuration = primaryRoute.optDouble("duration", 0.0)

            // 1. Parse route road geometry coordinates (geojson coordinates are [lon, lat])
            val geometryObj = primaryRoute.optJSONObject("geometry")
            val coordsArray = geometryObj?.optJSONArray("coordinates")
            val points = mutableListOf<GeoPoint>()

            if (coordsArray != null) {
                for (i in 0 until coordsArray.length()) {
                    val coord = coordsArray.getJSONArray(i)
                    val lon = coord.getDouble(0)
                    val lat = coord.getDouble(1)
                    points.add(GeoPoint(lat, lon))
                }
            }

            if (points.isEmpty()) {
                points.add(GeoPoint(startLat, startLng))
                points.add(GeoPoint(destLat, destLng))
            }

            // 2. Parse legs and turn-by-turn steps
            val legsArray = primaryRoute.optJSONArray("legs")
            val stepsList = mutableListOf<RouteStep>()
            var routeSummary = ""

            if (legsArray != null && legsArray.length() > 0) {
                val leg = legsArray.getJSONObject(0)
                routeSummary = leg.optString("summary", "").trim()

                val stepsJson = leg.optJSONArray("steps")
                if (stepsJson != null) {
                    for (i in 0 until stepsJson.length()) {
                        val stepObj = stepsJson.getJSONObject(i)
                        val stepDist = stepObj.optDouble("distance", 0.0)
                        val stepDuration = stepObj.optDouble("duration", 0.0)
                        val streetName = stepObj.optString("name", "").trim()

                        val maneuver = stepObj.optJSONObject("maneuver")
                        val maneuverType = maneuver?.optString("type", "turn") ?: "turn"
                        val maneuverModifier = maneuver?.optString("modifier")

                        val maneuverLoc = maneuver?.optJSONArray("location")
                        val stepGeo = if (maneuverLoc != null && maneuverLoc.length() >= 2) {
                            GeoPoint(maneuverLoc.getDouble(1), maneuverLoc.getDouble(0))
                        } else {
                            points.getOrNull(i.coerceAtMost(points.size - 1)) ?: GeoPoint(startLat, startLng)
                        }

                        val (instrBn, instrEn) = buildTurnInstruction(maneuverType, maneuverModifier, streetName, i == 0, i == stepsJson.length() - 1)

                        stepsList.add(
                            RouteStep(
                                id = i,
                                instructionBn = instrBn,
                                instructionEn = instrEn,
                                streetName = streetName,
                                distanceMeters = stepDist,
                                durationSeconds = stepDuration,
                                maneuverType = maneuverType,
                                maneuverModifier = maneuverModifier,
                                location = stepGeo
                            )
                        )
                    }
                }
            }

            if (routeSummary.isBlank() && stepsList.isNotEmpty()) {
                // Find most common named street for summary
                routeSummary = stepsList
                    .map { it.streetName }
                    .filter { it.isNotBlank() }
                    .groupBy { it }
                    .maxByOrNull { it.value.size }?.key ?: "প্রধান সড়ক"
            }

            RouteData(
                points = points,
                distanceMeters = totalDistance,
                durationSeconds = totalDuration,
                summary = routeSummary,
                steps = stepsList,
                isLoading = false,
                isFallback = false
            )
        } catch (e: Exception) {
            Log.e("OsrmRoutingService", "Error requesting OSRM route, falling back to direct line", e)
            createFallbackRoute(startLat, startLng, destLat, destLng, e.message ?: "Connection error")
        }
    }

    private fun createFallbackRoute(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double,
        errorReason: String
    ): RouteData {
        val directDistance = GeoUtils.calculateGeodesicDistanceMeters(startLat, startLng, destLat, destLng)
        // Road distance in Bangladesh cities typically ~1.25x - 1.35x geodesic distance
        val estimatedRoadDistance = directDistance * 1.30
        val estimatedDuration = (estimatedRoadDistance / 8.5) // ~30 km/h average city transit speed

        val points = listOf(
            GeoPoint(startLat, startLng),
            GeoPoint(destLat, destLng)
        )

        val fallbackSteps = listOf(
            RouteStep(
                id = 0,
                instructionBn = "যাত্রা শুরু করুন",
                instructionEn = "Depart and head towards destination",
                streetName = "",
                distanceMeters = estimatedRoadDistance,
                durationSeconds = estimatedDuration,
                maneuverType = "depart",
                maneuverModifier = null,
                location = GeoPoint(startLat, startLng)
            ),
            RouteStep(
                id = 1,
                instructionBn = "গন্তব্যে পৌঁছান",
                instructionEn = "Arrive at destination",
                streetName = "",
                distanceMeters = 0.0,
                durationSeconds = 0.0,
                maneuverType = "arrive",
                maneuverModifier = null,
                location = GeoPoint(destLat, destLng)
            )
        )

        return RouteData(
            points = points,
            distanceMeters = estimatedRoadDistance,
            durationSeconds = estimatedDuration,
            summary = "সরাসরি রুট",
            steps = fallbackSteps,
            isLoading = false,
            isFallback = true,
            errorMessage = errorReason
        )
    }

    private fun buildTurnInstruction(
        type: String,
        modifier: String?,
        streetName: String,
        isFirst: Boolean,
        isLast: Boolean
    ): Pair<String, String> {
        if (isLast || type == "arrive") {
            val bn = if (streetName.isNotBlank()) "$streetName এ গন্তব্যে পৌঁছাবেন" else "গন্তব্যে পৌঁছে গেছেন"
            val en = if (streetName.isNotBlank()) "Arrive at destination on $streetName" else "You have arrived at your destination"
            return Pair(bn, en)
        }

        if (isFirst || type == "depart") {
            val bn = if (streetName.isNotBlank()) "$streetName ধরে যাত্রা শুরু করুন" else "যাত্রা শুরু করুন"
            val en = if (streetName.isNotBlank()) "Head out onto $streetName" else "Head out on the route"
            return Pair(bn, en)
        }

        val (actionBn, actionEn) = when (modifier?.lowercase()) {
            "right" -> "ডানে মোড় নিন" to "Turn right"
            "left" -> "বামে মোড় নিন" to "Turn left"
            "slight right" -> "সামান্য ডানে যান" to "Keep slight right"
            "slight left" -> "সামান্য বামে যান" to "Keep slight left"
            "sharp right" -> "তীক্ষ্ণ ডানে মোড় নিন" to "Turn sharp right"
            "sharp left" -> "তীক্ষ্ণ বামে মোড় নিন" to "Turn sharp left"
            "uturn" -> "ইউ-টার্ন নিন" to "Make a U-turn"
            "straight" -> "সোজা চলুন" to "Continue straight"
            else -> when (type.lowercase()) {
                "roundabout", "rotary" -> "গোলচত্বর অতিক্রম করুন" to "Take the roundabout"
                "fork" -> "মোড়ে পথ ধরে চলুন" to "Take the fork"
                "end of road" -> "রাস্তার শেষে মোড় নিন" to "Turn at the end of the road"
                "merge" -> "মূল সড়কে যুক্ত হন" to "Merge onto the road"
                else -> "সোজা এগিয়ে যান" to "Continue ahead"
            }
        }

        val bn = if (streetName.isNotBlank()) {
            if (actionBn.contains("মোড়") || actionBn.contains("যান")) {
                "$streetName এ $actionBn"
            } else {
                "$streetName ধরে $actionBn"
            }
        } else {
            actionBn
        }

        val en = if (streetName.isNotBlank()) {
            "$actionEn onto $streetName"
        } else {
            actionEn
        }

        return Pair(bn, en)
    }
}
