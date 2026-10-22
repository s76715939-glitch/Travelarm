package com.example.engine

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.example.data.model.Destination
import com.example.data.model.DestinationCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class GeocodingHelper(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    /**
     * Search for places by query string using Android Geocoder,
     * with transparent OpenStreetMap Nominatim fallback (100% free, no billing or API key required).
     */
    suspend fun searchPlaces(query: String, maxResults: Int = 6): List<Destination> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        // 1. Try Android Geocoder first
        val androidResults = trySearchAndroidGeocoder(query, maxResults)
        if (androidResults.isNotEmpty()) {
            return@withContext androidResults
        }

        // 2. Fallback to free OpenStreetMap Nominatim search (works anywhere without API key or billing)
        return@withContext searchNominatim(query, maxResults)
    }

    private suspend fun trySearchAndroidGeocoder(query: String, maxResults: Int): List<Destination> {
        if (!Geocoder.isPresent()) return emptyList()

        return try {
            val geocoder = Geocoder(context, Locale("bn", "BD"))
            val addresses: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(query, maxResults) { list ->
                        continuation.resume(list)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, maxResults) ?: emptyList()
            }

            addresses.mapIndexed { index, address ->
                val featureName = address.featureName ?: query
                val fullAddress = (0..address.maxAddressLineIndex)
                    .mapNotNull { address.getAddressLine(it) }
                    .joinToString(", ")
                    .ifEmpty { "${address.locality ?: ""}, ${address.countryName ?: ""}" }

                Destination(
                    id = "geo_${System.currentTimeMillis()}_$index",
                    nameBn = featureName,
                    nameEn = featureName,
                    addressBn = fullAddress,
                    addressEn = fullAddress,
                    latitude = address.latitude,
                    longitude = address.longitude,
                    category = DestinationCategory.CUSTOM
                )
            }
        } catch (e: Exception) {
            Log.w("GeocodingHelper", "Android Geocoder search failed, will use Nominatim", e)
            emptyList()
        }
    }

    private fun searchNominatim(query: String, maxResults: Int): List<Destination> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedQuery&limit=$maxResults&accept-language=bn,en"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Travelarm/1.0 (Android App)")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return emptyList()

            val jsonArray = JSONArray(responseBody)
            val results = mutableListOf<Destination>()

            for (i in 0 until jsonArray.length().coerceAtMost(maxResults)) {
                val item = jsonArray.getJSONObject(i)
                val displayName = item.optString("display_name", query)
                val lat = item.optDouble("lat", 0.0)
                val lon = item.optDouble("lon", 0.0)
                val name = item.optString("name").ifBlank {
                    displayName.split(",").firstOrNull()?.trim() ?: query
                }

                if (lat != 0.0 && lon != 0.0) {
                    results.add(
                        Destination(
                            id = "osm_${item.optLong("osm_id", System.currentTimeMillis() + i)}",
                            nameBn = name,
                            nameEn = name,
                            addressBn = displayName,
                            addressEn = displayName,
                            latitude = lat,
                            longitude = lon,
                            category = DestinationCategory.CUSTOM
                        )
                    )
                }
            }
            results
        } catch (e: Exception) {
            Log.e("GeocodingHelper", "Nominatim search error", e)
            emptyList()
        }
    }

    /**
     * Reverse geocodes an exact point on the map (latitude, longitude) into an address.
     * Uses Android Geocoder with free Nominatim reverse fallback.
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Pair<String, String> = withContext(Dispatchers.IO) {
        // 1. Try Android Geocoder
        if (Geocoder.isPresent()) {
            try {
                val geocoder = Geocoder(context, Locale.forLanguageTag("bn-BD"))
                val addresses: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1) { list ->
                            continuation.resume(list)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1) ?: emptyList()
                }

                val address = addresses.firstOrNull()
                if (address != null) {
                    val placeName = address.featureName
                        ?: address.subLocality
                        ?: address.locality
                        ?: "নির্বাচিত স্থান"

                    val fullAddress = (0..address.maxAddressLineIndex)
                        .mapNotNull { address.getAddressLine(it) }
                        .joinToString(", ")
                        .ifEmpty { "${address.locality ?: ""}, ${address.adminArea ?: ""}" }

                    return@withContext Pair(placeName, fullAddress)
                }
            } catch (e: Exception) {
                Log.w("GeocodingHelper", "Android reverse geocoder failed, falling back to OSM", e)
            }
        }

        // 2. Nominatim Reverse Geocoding fallback (free, no billing)
        try {
            val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&accept-language=bn,en"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Travelarm/1.0 (Android App)")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            if (!responseBody.isNullOrBlank()) {
                val json = JSONObject(responseBody)
                val displayName = json.optString("display_name", "")
                val name = json.optString("name").ifBlank {
                    displayName.split(",").firstOrNull()?.trim() ?: "নির্বাচিত স্থান"
                }
                if (displayName.isNotBlank()) {
                    return@withContext Pair(name, displayName)
                }
            }
        } catch (e: Exception) {
            Log.e("GeocodingHelper", "Nominatim reverse error", e)
        }

        val coords = "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
        Pair("নির্বাচিত স্থান", coords)
    }
}
