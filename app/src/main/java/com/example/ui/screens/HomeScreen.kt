package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalWifiConnectedNoInternet4
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import com.example.data.local.JourneyEntity
import com.example.data.model.AppLanguage
import com.example.data.model.Destination
import com.example.data.model.DestinationCategory
import com.example.data.model.LocationReading
import com.example.engine.GeoUtils
import com.example.ui.components.BatteryOptimizationDialog
import com.example.ui.components.InteractiveMap
import org.osmdroid.util.GeoPoint
import androidx.compose.material.icons.filled.Directions
import com.example.data.model.RouteData

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    currentLocation: LocationReading?,
    selectedDestination: Destination?,
    thresholdMeters: Int,
    searchQuery: String,
    searchResults: List<Destination>,
    recentJourneys: List<JourneyEntity>,
    language: AppLanguage,
    routeData: RouteData? = null,
    isNetworkAvailable: Boolean = true,
    showBatteryOptimizationPopup: Boolean = false,
    onDismissBatteryPopup: () -> Unit = {},
    onRequestBatteryOptimization: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onDestinationSelect: (Destination) -> Unit,
    onDeselectDestination: () -> Unit = {},
    onMapTap: (lat: Double, lng: Double) -> Unit,
    onOpenConfirmSheet: () -> Unit,
    onRequestCurrentLocation: () -> Unit = {}
) {
    val context = LocalContext.current
    var isSearchExpanded by remember { mutableStateOf(false) }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    if (showBatteryOptimizationPopup) {
        BatteryOptimizationDialog(
            language = language,
            onDismiss = onDismissBatteryPopup,
            onOptimize = onRequestBatteryOptimization
        )
    }

    var searchPreviewDestination by remember { mutableStateOf<Destination?>(null) }
    var focusedLocation by remember { mutableStateOf<GeoPoint?>(null) }

    var bottomContentHeightPx by remember { mutableStateOf(0) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val bottomControlPadding = with(density) {
        if (bottomContentHeightPx > 0) {
            (bottomContentHeightPx.toDp() + 116.dp)
        } else if (selectedDestination != null || searchPreviewDestination != null) {
            300.dp
        } else {
            116.dp
        }
    }

    val filteredResults = searchResults

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp)
                .clipToBounds()
        ) {
            InteractiveMap(
                modifier = Modifier.fillMaxSize(),
                currentLocation = currentLocation,
                destination = selectedDestination,
                thresholdMeters = thresholdMeters,
                searchPreviewDestination = searchPreviewDestination,
                focusedLocation = focusedLocation,
                routeData = routeData,
                language = language,
                bottomControlPadding = bottomControlPadding,
                onMapTap = { lat, lng ->
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    searchPreviewDestination = null
                    focusedLocation = null
                    onMapTap(lat, lng)
                },
                onSelectDestination = onDestinationSelect,
                onDeselectDestination = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    searchPreviewDestination = null
                    focusedLocation = null
                    onDeselectDestination()
                },
                onRequestCurrentLocation = onRequestCurrentLocation
            )
        }

        // Top Search Bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            AnimatedVisibility(visible = !isNetworkAvailable) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("network_warning_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SignalWifiConnectedNoInternet4,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.BANGLA) "ইন্টারনেট ডাটা বন্ধ রয়েছে!" else "Internet Connection is Off!",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = if (language == AppLanguage.BANGLA) "ম্যাপ ও স্থান খোঁজার জন্য মোবাইল ডাটা চালু করুন" else "Please turn on data for maps and search",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    })
                                } catch (_: Exception) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        })
                                    } catch (_: Exception) {}
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.BANGLA) "ডাটা অন করুন" else "Turn On",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.ui.graphics.RectangleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = searchQuery,
                        onValueChange = {
                            onSearchQueryChange(it)
                            isSearchExpanded = it.isNotBlank()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("destination_search_input"),
                        placeholder = {
                            Text(
                                text = if (language == AppLanguage.BANGLA) "আপনার গন্তব্য খুজুন..." else "Search your destination...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        onSearchQueryChange("")
                                        isSearchExpanded = false
                                    },
                                    modifier = Modifier.testTag("clear_search_button")
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent
                        )
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                }
            }

            AnimatedVisibility(visible = isSearchExpanded || searchQuery.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(vertical = 4.dp).height(240.dp)) {
                        items(filteredResults) { dest ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        focusedLocation = GeoPoint(dest.latitude, dest.longitude)
                                        searchPreviewDestination = dest
                                        isSearchExpanded = false
                                        onSearchQueryChange("")
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("search_result_${dest.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (language == AppLanguage.BANGLA) dest.nameBn else dest.nameEn,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (language == AppLanguage.BANGLA) dest.addressBn else dest.addressEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Destination Card & Action Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 96.dp)
                .onSizeChanged { size ->
                    bottomContentHeightPx = size.height
                }
        ) {
            if (selectedDestination != null) {
                val currentDist = if (routeData != null && routeData.points.size >= 2 && currentLocation != null) {
                    GeoUtils.calculateRemainingRouteDistanceMeters(
                        currentLocation.latitude,
                        currentLocation.longitude,
                        routeData.points
                    )
                } else if (routeData != null && routeData.distanceMeters > 0) {
                    routeData.distanceMeters
                } else if (currentLocation != null) {
                    GeoUtils.calculateGeodesicDistanceMeters(
                        currentLocation.latitude, currentLocation.longitude,
                        selectedDestination.latitude, selectedDestination.longitude
                    )
                } else {
                    null
                }

                var initialDistance by remember(selectedDestination) {
                    mutableStateOf(currentDist ?: 5000.0)
                }
                if (currentDist != null && currentDist > initialDistance) {
                    initialDistance = currentDist
                }

                val progress = if (currentDist != null && initialDistance > 0) {
                    val traveled = initialDistance - currentDist
                    (traveled / initialDistance).coerceIn(0.0, 1.0).toFloat()
                } else {
                    0f
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.NearMe,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.BANGLA) selectedDestination.nameBn else selectedDestination.nameEn,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                val distText = if (currentDist != null) {
                                    " • ${if (language == AppLanguage.BANGLA) "দূরত্ব" else "Distance"}: ${GeoUtils.formatDistance(currentDist, language)}"
                                } else ""
                                Text(
                                    text = "${if (language == AppLanguage.BANGLA) selectedDestination.addressBn else selectedDestination.addressEn}$distText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    searchPreviewDestination = null
                                    focusedLocation = null
                                    onDeselectDestination()
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("deselect_destination_button")
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Deselect",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == AppLanguage.BANGLA) "বাতিল" else "Deselect",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        if (currentDist != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BANGLA) "গন্তব্যের অগ্রগতি" else "Trip Progress",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${if (language == AppLanguage.BANGLA) "বাকি:" else "Remaining:"} ${GeoUtils.formatDistance(currentDist, language)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .testTag("destination_progress_bar"),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        }
                    }
                }
            } else if (searchPreviewDestination != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.BANGLA) searchPreviewDestination!!.nameBn else searchPreviewDestination!!.nameEn,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (language == AppLanguage.BANGLA) "গন্তব্য হিসেবে নির্ধারণ করতে মানচিত্রে ট্যাপ করুন" else "Tap on map to set this as destination",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(onClick = {
                            searchPreviewDestination = null
                            focusedLocation = null
                            onDeselectDestination()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (selectedDestination != null) {
                Button(
                    onClick = { onOpenConfirmSheet() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("select_destination_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.BANGLA) "যাত্রার বিবরণ ও অ্যালার্ম সেট করুন" else "Set Alarm & Start Journey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (searchPreviewDestination != null) {
                Button(
                    onClick = {
                        onDestinationSelect(searchPreviewDestination!!)
                        searchPreviewDestination = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("confirm_destination_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.BANGLA) "গন্তব্য হিসেবে নিশ্চিত করুন" else "Confirm as Destination",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
