package com.example.ui.components

import android.preference.PreferenceManager
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.AppLanguage
import com.example.data.model.Destination
import com.example.data.model.LocationReading
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import com.example.engine.GeoUtils
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.CircularProgressIndicator
import com.example.data.model.RouteData
import org.osmdroid.util.BoundingBox

@Composable
fun InteractiveMap(
    modifier: Modifier = Modifier,
    currentLocation: LocationReading?,
    destination: Destination?,
    thresholdMeters: Int,
    searchPreviewDestination: Destination? = null,
    focusedLocation: GeoPoint? = null,
    routeData: RouteData? = null,
    isNavigating: Boolean = false,
    language: AppLanguage = AppLanguage.BANGLA,
    bottomControlPadding: androidx.compose.ui.unit.Dp = 140.dp,
    centerOnLocationTrigger: Int = 0,
    onMapTap: (latitude: Double, longitude: Double) -> Unit,
    onSelectDestination: (Destination) -> Unit,
    onDeselectDestination: () -> Unit = {},
    onRequestCurrentLocation: () -> Unit = {},
    onMapInteracted: () -> Unit = {}
) {
    val context = LocalContext.current
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var locationToastVisible by remember { mutableStateOf(false) }
    var useTopoTile by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
    }

    var isUserPannedAway by remember(isNavigating) { mutableStateOf(false) }
    var hasCenteredOnInitialLocation by remember { mutableStateOf(false) }

    // Auto-center map on user location when app opens / location becomes available
    LaunchedEffect(currentLocation, mapViewInstance) {
        if (!hasCenteredOnInitialLocation && currentLocation != null && mapViewInstance != null) {
            hasCenteredOnInitialLocation = true
            mapViewInstance?.controller?.animateTo(
                GeoPoint(currentLocation.latitude, currentLocation.longitude),
                15.5,
                900L
            )
        }
    }

    // Re-center when bottom navigation Map tab is clicked
    LaunchedEffect(centerOnLocationTrigger) {
        if (centerOnLocationTrigger > 0 && currentLocation != null && mapViewInstance != null) {
            mapViewInstance?.controller?.animateTo(
                GeoPoint(currentLocation.latitude, currentLocation.longitude),
                15.5,
                900L
            )
            locationToastVisible = true
        }
    }

    LaunchedEffect(currentLocation, isNavigating, isUserPannedAway, mapViewInstance) {
        if (currentLocation != null) {
            if (isNavigating && !isUserPannedAway && mapViewInstance != null) {
                val currentZoom = mapViewInstance?.zoomLevelDouble ?: 13.5
                val navZoom = if (currentZoom < 17.5) 18.2 else currentZoom
                mapViewInstance?.controller?.animateTo(
                    GeoPoint(currentLocation.latitude, currentLocation.longitude),
                    navZoom,
                    450L
                )
            }
        }
    }

    LaunchedEffect(focusedLocation) {
        if (!isNavigating && focusedLocation != null && mapViewInstance != null) {
            mapViewInstance?.controller?.animateTo(focusedLocation, 15.5, 900L)
        }
    }

    LaunchedEffect(destination, isNavigating) {
        if (!isNavigating && destination != null && mapViewInstance != null && currentLocation != null) {
            try {
                val pair = listOf(
                    GeoPoint(currentLocation.latitude, currentLocation.longitude),
                    GeoPoint(destination.latitude, destination.longitude)
                )
                val bb = BoundingBox.fromGeoPoints(pair)
                mapViewInstance?.zoomToBoundingBox(bb, true, 200)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(routeData?.points, isNavigating) {
        val points = routeData?.points
        if (!isNavigating && points != null && points.size >= 2 && mapViewInstance != null) {
            try {
                val bb = BoundingBox.fromGeoPoints(points)
                mapViewInstance?.zoomToBoundingBox(bb, true, 180)
            } catch (_: Exception) {}
        }
    }

    var prevDestination by remember { mutableStateOf<Destination?>(null) }
    var prevSearchPreview by remember { mutableStateOf<Destination?>(null) }

    LaunchedEffect(destination, searchPreviewDestination) {
        val destinationCleared = (destination == null && prevDestination != null)
        val previewCleared = (searchPreviewDestination == null && prevSearchPreview != null && destination == null)

        if (!isNavigating && (destinationCleared || previewCleared) && mapViewInstance != null) {
            val targetGeoPoint = if (currentLocation != null) {
                GeoPoint(currentLocation.latitude, currentLocation.longitude)
            } else {
                GeoPoint(23.7779, 90.3475)
            }
            mapViewInstance?.controller?.animateTo(targetGeoPoint, 16.0, 750L)
        }

        prevDestination = destination
        prevSearchPreview = searchPreviewDestination
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewInstance?.onDetach()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("osm_map_view"),
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = ctx.packageName
                Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(false)

                    val initialLat = currentLocation?.latitude ?: 23.7779
                    val initialLng = currentLocation?.longitude ?: 90.3475
                    val initialZoom = if (currentLocation != null) 15.5 else 13.5
                    controller.setZoom(initialZoom)
                    controller.setCenter(GeoPoint(initialLat, initialLng))

                    val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                onMapTap(p.latitude, p.longitude)
                            }
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean = false
                    })
                    overlays.add(0, mapEventsOverlay)

                    mapViewInstance = this
                    setOnTouchListener { _, event ->
                        onMapInteracted()
                        if (event.action == android.view.MotionEvent.ACTION_MOVE || event.action == android.view.MotionEvent.ACTION_POINTER_DOWN) {
                            if (isNavigating) {
                                isUserPannedAway = true
                            }
                        }
                        false
                    }
                }
            },
            update = { mapView ->
                mapViewInstance = mapView

                val eventsOverlay = mapView.overlays.firstOrNull { it is MapEventsOverlay }
                mapView.overlays.clear()
                if (eventsOverlay != null) {
                    mapView.overlays.add(eventsOverlay)
                }

                // 1. User Location Pin
                if (currentLocation != null) {
                    val userPoint = GeoPoint(currentLocation.latitude, currentLocation.longitude)
                    val userMarker = Marker(mapView).apply {
                        position = userPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        if (isNavigating) {
                            icon = ContextCompat.getDrawable(mapView.context, com.example.R.drawable.ic_nav_arrow)
                            val routeBearing = com.example.engine.GeoUtils.calculateRouteBearing(
                                currentLat = currentLocation.latitude,
                                currentLng = currentLocation.longitude,
                                routePoints = routeData?.points,
                                destLat = destination?.latitude,
                                destLng = destination?.longitude,
                                currentBearing = currentLocation.bearing
                            )
                            rotation = routeBearing
                        } else {
                            icon = ContextCompat.getDrawable(mapView.context, com.example.R.drawable.ic_start_pin)
                            rotation = 0f
                        }
                        title = if (language == AppLanguage.BANGLA) "আমার বর্তমান অবস্থান" else "My Current Location"
                        snippet = "${if (language == AppLanguage.BANGLA) "গতি" else "Speed"}: ${String.format("%.1f", currentLocation.speedKmh)} ${if (language == AppLanguage.BANGLA) "কিমি/ঘ" else "km/h"}"
                        setOnMarkerClickListener { _, _ -> false }
                    }
                    mapView.overlays.add(userMarker)
                }

                // 2. Search Preview Candidate Pin
                if (destination == null && searchPreviewDestination != null) {
                    val previewPoint = GeoPoint(searchPreviewDestination.latitude, searchPreviewDestination.longitude)
                    val previewMarker = Marker(mapView).apply {
                        position = previewPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = ContextCompat.getDrawable(mapView.context, com.example.R.drawable.ic_map_pin)
                        title = if (language == AppLanguage.BANGLA) searchPreviewDestination.nameBn else searchPreviewDestination.nameEn
                        snippet = if (language == AppLanguage.BANGLA) "গন্তব্য হিসেবে নির্বাচন করতে ট্যাপ করুন" else "Tap to select as destination"
                        setOnMarkerClickListener { _, _ ->
                            onMapTap(searchPreviewDestination.latitude, searchPreviewDestination.longitude)
                            true
                        }
                    }
                    mapView.overlays.add(previewMarker)
                }

                // 3. Selected Destination, Threshold Circle, and Road Polyline
                if (destination != null) {
                    val destPoint = GeoPoint(destination.latitude, destination.longitude)

                    val destMarker = Marker(mapView).apply {
                        position = destPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = ContextCompat.getDrawable(mapView.context, com.example.R.drawable.ic_map_pin)
                        title = if (language == AppLanguage.BANGLA) destination.nameBn else destination.nameEn
                        snippet = if (language == AppLanguage.BANGLA) destination.addressBn else destination.addressEn
                        setOnMarkerClickListener { m, _ ->
                            onMapTap(m.position.latitude, m.position.longitude)
                            true
                        }
                    }
                    mapView.overlays.add(destMarker)

                    val circlePolygon = Polygon().apply {
                        points = Polygon.pointsAsCircle(destPoint, thresholdMeters.toDouble())
                        fillPaint.color = android.graphics.Color.argb(55, 231, 111, 81)
                        outlinePaint.color = android.graphics.Color.rgb(186, 26, 26)
                        outlinePaint.strokeWidth = 5f
                        setOnClickListener { _, _, eventPos ->
                            onMapTap(eventPos.latitude, eventPos.longitude)
                            true
                        }
                    }
                    mapView.overlays.add(circlePolygon)

                    val points = routeData?.points
                    if (points != null && points.size >= 2) {
                        // Use optimized interpolated route from GeoUtils
                        val trimmedPoints = if (currentLocation != null) {
                            val userGeoPoint = GeoPoint(currentLocation.latitude, currentLocation.longitude)
                            GeoUtils.getInterpolatedRoute(userGeoPoint, points)
                        } else {
                            points
                        }

                        val casingLine = Polyline().apply {
                            setPoints(trimmedPoints)
                            outlinePaint.color = android.graphics.Color.rgb(13, 71, 161)
                            outlinePaint.strokeWidth = 13f
                            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                            outlinePaint.isAntiAlias = true
                            setOnClickListener { _, _, eventPos ->
                                onMapTap(eventPos.latitude, eventPos.longitude)
                                true
                            }
                        }
                        mapView.overlays.add(casingLine)

                        val coreLine = Polyline().apply {
                            setPoints(trimmedPoints)
                            outlinePaint.color = android.graphics.Color.rgb(26, 115, 232)
                            outlinePaint.strokeWidth = 8f
                            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                            outlinePaint.isAntiAlias = true
                            setOnClickListener { _, _, eventPos ->
                                onMapTap(eventPos.latitude, eventPos.longitude)
                                true
                            }
                        }
                        mapView.overlays.add(coreLine)
                    } else if (currentLocation != null && !isNavigating) {
                        val fallbackLine = Polyline().apply {
                            addPoint(GeoPoint(currentLocation.latitude, currentLocation.longitude))
                            addPoint(destPoint)
                            outlinePaint.color = android.graphics.Color.rgb(26, 115, 232)
                            outlinePaint.strokeWidth = 8f
                            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                            setOnClickListener { _, _, eventPos ->
                                onMapTap(eventPos.latitude, eventPos.longitude)
                                true
                            }
                        }
                        mapView.overlays.add(fallbackLine)
                    }
                }

                mapView.invalidate()
            }
        )

        // Right-side Control Buttons (Hidden during navigation)
        if (!isNavigating) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = bottomControlPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        onMapInteracted()
                        useTopoTile = !useTopoTile
                        mapViewInstance?.setTileSource(
                            if (useTopoTile) TileSourceFactory.OpenTopo else TileSourceFactory.MAPNIK
                        )
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("toggle_map_type_button"),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    elevation = FloatingActionButtonDefaults.elevation(3.dp)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Toggle Map Style", modifier = Modifier.size(22.dp))
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 3.dp
                ) {
                    Column {
                        IconButton(
                            onClick = {
                                onMapInteracted()
                                mapViewInstance?.controller?.zoomIn()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("zoom_in_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In")
                        }
                        IconButton(
                            onClick = {
                                onMapInteracted()
                                mapViewInstance?.controller?.zoomOut()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("zoom_out_button")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                        }
                    }
                }

                FloatingActionButton(
                    onClick = {
                        onMapInteracted()
                        onRequestCurrentLocation()
                        if (currentLocation != null) {
                            mapViewInstance?.controller?.animateTo(
                                GeoPoint(currentLocation.latitude, currentLocation.longitude),
                                15.5,
                                900L
                            )
                        }
                        locationToastVisible = true
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("my_location_button"),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "Current Device Location",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        if (isNavigating && isUserPannedAway) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (destination != null) 360.dp else 160.dp)
            ) {
                androidx.compose.material3.Button(
                    onClick = {
                        isUserPannedAway = false
                        if (currentLocation != null && mapViewInstance != null) {
                            mapViewInstance?.controller?.animateTo(
                                GeoPoint(currentLocation.latitude, currentLocation.longitude),
                                17.5,
                                600L
                            )
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(6.dp),
                    modifier = Modifier.testTag("recenter_route_button")
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.BANGLA) "রুট অনুসরণ করুন" else "Re-center Route",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = locationToastVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 135.dp)
        ) {
            LaunchedEffect(locationToastVisible) {
                kotlinx.coroutines.delay(2000)
                locationToastVisible = false
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xDD0D3B3E),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationSearching,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentLocation != null) {
                            if (language == AppLanguage.BANGLA) "বর্তমান লোকেশন সেন্টারে নেওয়া হয়েছে" else "Centered on current location"
                        } else {
                            if (language == AppLanguage.BANGLA) "জিপিএস সিগন্যাল অনুসন্ধান করা হচ্ছে..." else "Searching for GPS signal..."
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
