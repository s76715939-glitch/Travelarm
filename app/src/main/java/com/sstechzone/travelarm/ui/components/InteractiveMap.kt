package com.sstechzone.travelarm.ui.components

import android.content.Context
import org.osmdroid.views.CustomZoomButtonsController
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
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
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
import com.sstechzone.travelarm.data.model.AppLanguage
import com.sstechzone.travelarm.data.model.Destination
import com.sstechzone.travelarm.data.model.LocationReading
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import com.sstechzone.travelarm.engine.GeoUtils
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Directions
import com.sstechzone.travelarm.data.model.RouteData
import com.sstechzone.travelarm.data.model.MapType
import org.osmdroid.util.BoundingBox

enum class NavigationCameraMode {
    ROUTE_OVERVIEW,
    USER_FOCUS
}

data class CameraFraming(
    val center: GeoPoint,
    val zoom: Double
)

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
    mapType: MapType = MapType.STREET,
    onMapTypeChange: (MapType) -> Unit = {},
    bottomControlPadding: androidx.compose.ui.unit.Dp = 140.dp,
    recenterBottomPadding: androidx.compose.ui.unit.Dp = 260.dp,
    activeJourneyBottomCardHeight: androidx.compose.ui.unit.Dp = 260.dp,
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

    var navCameraMode by remember(isNavigating) { mutableStateOf(NavigationCameraMode.USER_FOCUS) }
    var lastFramedCenter by remember { mutableStateOf<GeoPoint?>(null) }
    var lastFramedZoom by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(mapType, mapViewInstance) {
        val mv = mapViewInstance ?: return@LaunchedEffect
        val tileSource = when (mapType) {
            MapType.STREET -> TileSourceFactory.MAPNIK
            MapType.TOPOGRAPHIC -> TileSourceFactory.OpenTopo
            MapType.SATELLITE -> TileSourceFactory.USGS_SAT
        }
        mv.setTileSource(tileSource)
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val navCenterOffsetY = with(density) { 140.dp.roundToPx() }
    val topInsetPx = with(density) { 85.dp.roundToPx() }
    val bottomInsetPx = with(density) { (activeJourneyBottomCardHeight + 20.dp).roundToPx() }
    val sideMarginPx = with(density) { 36.dp.roundToPx() }

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().load(context, context.getSharedPreferences("osm_prefs", Context.MODE_PRIVATE))
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

    // Dynamic auto-follow during navigation:
    // When in ROUTE_OVERVIEW mode (default): automatically centers and adjusts zoom level to keep both the
    // user's location pointer and the destination route visible in the unobstructed viewport.
    // When in USER_FOCUS mode: close-up turn-by-turn follow.
    LaunchedEffect(currentLocation, isNavigating, isUserPannedAway, navCameraMode, mapViewInstance, routeData?.points, destination) {
        if (currentLocation != null && mapViewInstance != null) {
            if (isNavigating && !isUserPannedAway) {
                val mv = mapViewInstance ?: return@LaunchedEffect

                if (navCameraMode == NavigationCameraMode.ROUTE_OVERVIEW) {
                    val framing = calculateActiveJourneyFraming(
                        mapView = mv,
                        currentLocation = currentLocation,
                        destination = destination,
                        routePoints = routeData?.points,
                        topInsetPx = topInsetPx,
                        bottomInsetPx = bottomInsetPx,
                        sideMarginPx = sideMarginPx
                    )

                    val prevCenter = lastFramedCenter
                    val prevZoom = lastFramedZoom

                    val distMoved = if (prevCenter != null) {
                        GeoUtils.calculateGeodesicDistanceMeters(
                            prevCenter.latitude, prevCenter.longitude,
                            framing.center.latitude, framing.center.longitude
                        )
                    } else Double.MAX_VALUE

                    val zoomDiff = if (prevZoom != null) kotlin.math.abs(framing.zoom - prevZoom) else Double.MAX_VALUE

                    if (distMoved > 12.0 || zoomDiff > 0.22) {
                        lastFramedCenter = framing.center
                        lastFramedZoom = framing.zoom
                        mv.controller.animateTo(framing.center, framing.zoom, 550L)
                    }
                } else {
                    val displayPoint = GeoUtils.snapToRoute(
                        currentLat = currentLocation.latitude,
                        currentLng = currentLocation.longitude,
                        routePoints = routeData?.points
                    )
                    val currentZoom = mv.zoomLevelDouble
                    val navZoom = if (currentZoom < 17.0) 17.8 else currentZoom
                    val targetCenter = calculateNavigationCenter(mv, displayPoint, navCenterOffsetY)
                    mv.controller.animateTo(targetCenter, navZoom, 450L)
                }
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
                Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osm_prefs", Context.MODE_PRIVATE))

                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

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
                    val displayPoint = if (isNavigating) {
                        GeoUtils.snapToRoute(
                            currentLat = currentLocation.latitude,
                            currentLng = currentLocation.longitude,
                            routePoints = routeData?.points
                        )
                    } else {
                        GeoPoint(currentLocation.latitude, currentLocation.longitude)
                    }
                    val userMarker = Marker(mapView).apply {
                        position = displayPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        if (isNavigating) {
                            icon = ContextCompat.getDrawable(mapView.context, com.sstechzone.travelarm.R.drawable.ic_nav_arrow)
                            val routeBearing = com.sstechzone.travelarm.engine.GeoUtils.calculateRouteBearing(
                                currentLat = displayPoint.latitude,
                                currentLng = displayPoint.longitude,
                                routePoints = routeData?.points,
                                destLat = destination?.latitude,
                                destLng = destination?.longitude,
                                currentBearing = currentLocation.bearing
                            )
                            rotation = routeBearing
                        } else {
                            icon = ContextCompat.getDrawable(mapView.context, com.sstechzone.travelarm.R.drawable.ic_start_pin)
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
                        icon = ContextCompat.getDrawable(mapView.context, com.sstechzone.travelarm.R.drawable.ic_map_pin)
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
                        icon = ContextCompat.getDrawable(mapView.context, com.sstechzone.travelarm.R.drawable.ic_map_pin)
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
                        // Use optimized interpolated route from GeoUtils starting from snapped displayPoint
                        val navStartPoint = if (currentLocation != null && isNavigating) {
                            GeoUtils.snapToRoute(currentLocation.latitude, currentLocation.longitude, points)
                        } else if (currentLocation != null) {
                            GeoPoint(currentLocation.latitude, currentLocation.longitude)
                        } else null

                        val trimmedPoints = if (navStartPoint != null) {
                            GeoUtils.getInterpolatedRoute(navStartPoint, points)
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

        // Right-side Control Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(
                    end = 16.dp,
                    bottom = if (isNavigating) recenterBottomPadding + 4.dp else bottomControlPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isNavigating) {
                // Camera View Mode Toggle Button during Active Journey
                FloatingActionButton(
                    onClick = {
                        onMapInteracted()
                        val nextMode = if (navCameraMode == NavigationCameraMode.ROUTE_OVERVIEW) {
                            NavigationCameraMode.USER_FOCUS
                        } else {
                            NavigationCameraMode.ROUTE_OVERVIEW
                        }
                        navCameraMode = nextMode
                        isUserPannedAway = false
                        val mv = mapViewInstance
                        if (mv != null && currentLocation != null) {
                            if (nextMode == NavigationCameraMode.ROUTE_OVERVIEW) {
                                val framing = calculateActiveJourneyFraming(
                                    mapView = mv,
                                    currentLocation = currentLocation,
                                    destination = destination,
                                    routePoints = routeData?.points,
                                    topInsetPx = topInsetPx,
                                    bottomInsetPx = bottomInsetPx,
                                    sideMarginPx = sideMarginPx
                                )
                                lastFramedCenter = framing.center
                                lastFramedZoom = framing.zoom
                                mv.controller.animateTo(framing.center, framing.zoom, 600L)
                            } else {
                                val displayPoint = GeoUtils.snapToRoute(
                                    currentLat = currentLocation.latitude,
                                    currentLng = currentLocation.longitude,
                                    routePoints = routeData?.points
                                )
                                val targetCenter = calculateNavigationCenter(mv, displayPoint, navCenterOffsetY)
                                mv.controller.animateTo(targetCenter, 17.8, 500L)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("nav_camera_mode_toggle"),
                    containerColor = if (navCameraMode == NavigationCameraMode.ROUTE_OVERVIEW) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    },
                    contentColor = if (navCameraMode == NavigationCameraMode.ROUTE_OVERVIEW) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    elevation = FloatingActionButtonDefaults.elevation(3.dp)
                ) {
                    Icon(
                        imageVector = if (navCameraMode == NavigationCameraMode.ROUTE_OVERVIEW) Icons.Default.Directions else Icons.Default.Navigation,
                        contentDescription = if (language == AppLanguage.BANGLA) "ক্যামেরা মোড পরিবর্তন" else "Toggle Camera Mode",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (!isNavigating) {
                FloatingActionButton(
                    onClick = {
                        onMapInteracted()
                        val nextType = when (mapType) {
                            MapType.STREET -> MapType.TOPOGRAPHIC
                            MapType.TOPOGRAPHIC -> MapType.SATELLITE
                            MapType.SATELLITE -> MapType.STREET
                        }
                        onMapTypeChange(nextType)
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
                            if (isNavigating) isUserPannedAway = true
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
                            if (isNavigating) isUserPannedAway = true
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

            if (!isNavigating) {
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
                    .padding(bottom = recenterBottomPadding)
            ) {
                androidx.compose.material3.Button(
                    onClick = {
                        isUserPannedAway = false
                        if (currentLocation != null && mapViewInstance != null) {
                            val mv = mapViewInstance!!
                            if (navCameraMode == NavigationCameraMode.ROUTE_OVERVIEW) {
                                val framing = calculateActiveJourneyFraming(
                                    mapView = mv,
                                    currentLocation = currentLocation,
                                    destination = destination,
                                    routePoints = routeData?.points,
                                    topInsetPx = topInsetPx,
                                    bottomInsetPx = bottomInsetPx,
                                    sideMarginPx = sideMarginPx
                                )
                                lastFramedCenter = framing.center
                                lastFramedZoom = framing.zoom
                                mv.controller.animateTo(framing.center, framing.zoom, 600L)
                            } else {
                                val displayPoint = GeoUtils.snapToRoute(
                                    currentLat = currentLocation.latitude,
                                    currentLng = currentLocation.longitude,
                                    routePoints = routeData?.points
                                )
                                val targetCenter = calculateNavigationCenter(mv, displayPoint, navCenterOffsetY)
                                mv.controller.animateTo(targetCenter, 17.8, 500L)
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(6.dp),
                    modifier = Modifier.testTag("recenter_route_button")
                ) {
                    Icon(
                        if (navCameraMode == NavigationCameraMode.ROUTE_OVERVIEW) Icons.Default.Directions else Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
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

/**
 * Offsets the target point southward so that it renders in the upper-middle region
 * of the screen above the active journey bottom card.
 */
private fun calculateNavigationCenter(
    mapView: MapView,
    targetPoint: GeoPoint,
    offsetYPixels: Int
): GeoPoint {
    val proj = mapView.projection
    if (proj != null && offsetYPixels > 0) {
        try {
            val p = proj.toPixels(targetPoint, null)
            val centerPix = android.graphics.Point(p.x, p.y + offsetYPixels)
            val offsetGeo = proj.fromPixels(centerPix.x, centerPix.y)
            if (offsetGeo is GeoPoint) return offsetGeo
        } catch (_: Exception) {}
    }
    // Geodesic offset fallback: at zoom 18, 1 pixel is approx 0.55m
    val metersOffset = offsetYPixels * 0.55
    val latDelta = metersOffset / 111000.0
    return GeoPoint(targetPoint.latitude - latDelta, targetPoint.longitude)
}

/**
 * Calculates camera target center (GeoPoint) and zoom level (Double)
 * to keep both the user's location pointer and the destination route (remaining path)
 * completely visible inside the unobstructed map viewport between the top status/GPS bar
 * and the active journey bottom card.
 */
private fun calculateActiveJourneyFraming(
    mapView: MapView,
    currentLocation: LocationReading,
    destination: Destination?,
    routePoints: List<GeoPoint>?,
    topInsetPx: Int,
    bottomInsetPx: Int,
    sideMarginPx: Int
): CameraFraming {
    val navStartPoint = if (!routePoints.isNullOrEmpty()) {
        GeoUtils.snapToRoute(currentLocation.latitude, currentLocation.longitude, routePoints)
    } else {
        GeoPoint(currentLocation.latitude, currentLocation.longitude)
    }

    val corridorPoints = mutableListOf<GeoPoint>()
    corridorPoints.add(navStartPoint)

    if (destination != null) {
        corridorPoints.add(GeoPoint(destination.latitude, destination.longitude))
    }

    if (!routePoints.isNullOrEmpty() && routePoints.size >= 2) {
        val remaining = GeoUtils.getInterpolatedRoute(navStartPoint, routePoints)
        if (remaining.isNotEmpty()) {
            val step = (remaining.size / 35).coerceAtLeast(1)
            for (i in remaining.indices step step) {
                corridorPoints.add(remaining[i])
            }
            corridorPoints.add(remaining.last())
        }
    }

    var minLat = Double.MAX_VALUE
    var maxLat = -Double.MAX_VALUE
    var minLon = Double.MAX_VALUE
    var maxLon = -Double.MAX_VALUE

    for (pt in corridorPoints) {
        if (pt.latitude < minLat) minLat = pt.latitude
        if (pt.latitude > maxLat) maxLat = pt.latitude
        if (pt.longitude < minLon) minLon = pt.longitude
        if (pt.longitude > maxLon) maxLon = pt.longitude
    }

    // Safety margins around bounding box
    val latSpanRaw = maxLat - minLat
    val lonSpanRaw = maxLon - minLon
    val latMargin = (latSpanRaw * 0.12).coerceAtLeast(0.0012)
    val lonMargin = (lonSpanRaw * 0.12).coerceAtLeast(0.0012)

    minLat -= latMargin
    maxLat += latMargin
    minLon -= lonMargin
    maxLon += lonMargin

    val corridorCenterLat = (minLat + maxLat) / 2.0
    val corridorCenterLon = (minLon + maxLon) / 2.0
    val corridorCenter = GeoPoint(corridorCenterLat, corridorCenterLon)

    val spanLat = (maxLat - minLat).coerceAtLeast(0.0006)
    val spanLon = (maxLon - minLon).coerceAtLeast(0.0006)

    val mapWidth = if (mapView.width > 0) mapView.width else 1080
    val mapHeight = if (mapView.height > 0) mapView.height else 2160

    val visibleWidth = (mapWidth - 2 * sideMarginPx).coerceAtLeast(200)
    val visibleHeight = (mapHeight - topInsetPx - bottomInsetPx).coerceAtLeast(200)

    val radLat = Math.toRadians(corridorCenterLat)
    val cosLat = kotlin.math.cos(radLat).coerceAtLeast(0.15)

    val zoomH = kotlin.math.ln((visibleHeight * 360.0 * cosLat) / (256.0 * spanLat)) / kotlin.math.ln(2.0)
    val zoomW = kotlin.math.ln((visibleWidth * 360.0) / (256.0 * spanLon)) / kotlin.math.ln(2.0)
    val calculatedZoom = kotlin.math.min(zoomH, zoomW)

    val clampedZoom = calculatedZoom.coerceIn(10.5, 17.5)

    // Vertical shift to center corridor within the visible region between top bar and bottom card
    val shiftPixelsY = (bottomInsetPx - topInsetPx) / 2
    val targetCenter = calculateNavigationCenter(mapView, corridorCenter, shiftPixelsY)

    return CameraFraming(center = targetCenter, zoom = clampedZoom)
}
