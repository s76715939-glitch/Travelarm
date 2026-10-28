package com.sstechzone.travelarm.engine

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class RouteResult(
    val points: List<Pair<Double, Double>>, // Lat, Lng
    val distanceMeters: Double,
    val durationSeconds: Double
)

object RoutingEngine {
    private val client = OkHttpClient()

    suspend fun fetchRoute(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double
    ): RouteResult? = withContext(Dispatchers.IO) {
        try {
            // OSRM Public API endpoint for real road navigation geometry
            val url = "https://router.project-osrm.org/route/v1/driving/$startLng,$startLat;$destLng,$destLat?overview=full&geometries=geojson&alternatives=true"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null

            val json = JSONObject(body)
            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) return@withContext null

            // Find the route with the minimum distance
            var bestRouteObj = routes.getJSONObject(0)
            var minDistance = bestRouteObj.getDouble("distance")

            for (i in 1 until routes.length()) {
                val route = routes.getJSONObject(i)
                val distance = route.getDouble("distance")
                if (distance < minDistance) {
                    minDistance = distance
                    bestRouteObj = route
                }
            }

            val distance = bestRouteObj.getDouble("distance") // in meters
            val duration = bestRouteObj.getDouble("duration") // in seconds

            val geometry = bestRouteObj.getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")

            val points = mutableListOf<Pair<Double, Double>>()
            for (i in 0 until coordinates.length()) {
                val coord = coordinates.getJSONArray(i)
                val lng = coord.getDouble(0)
                val lat = coord.getDouble(1)
                points.add(Pair(lat, lng))
            }

            RouteResult(points, distance, duration)
        } catch (e: Exception) {
            Log.e("RoutingEngine", "Error fetching route from OSRM", e)
            null
        }
    }
}
