package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveJourneyState
import com.example.data.model.AppLanguage
import com.example.data.model.GpsSignalQuality
import com.example.data.model.JourneyStatus
import com.example.data.model.LocationReading
import com.example.engine.GeoUtils
import com.example.ui.components.InteractiveMap

@Composable
fun ActiveJourneyScreen(
    modifier: Modifier = Modifier,
    journeyState: ActiveJourneyState,
    language: AppLanguage,
    onEndJourney: () -> Unit,
    onFastForwardSimulator: () -> Unit,
    onDismissAlarm: () -> Unit = {}
) {
    val destination = journeyState.destination
    val destName = if (language == AppLanguage.BANGLA) destination?.nameBn ?: "গন্তব্য" else destination?.nameEn ?: "Destination"

    val infiniteTransition = rememberInfiniteTransition(label = "TrackingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TrackingPulseAlpha"
    )

    val progress = if (journeyState.initialTotalDistanceMeters > 0) {
        val traveled = journeyState.initialTotalDistanceMeters - journeyState.remainingDistanceMeters
        (traveled / journeyState.initialTotalDistanceMeters).coerceIn(0.0, 1.0).toFloat()
    } else 0f

    Box(modifier = modifier.fillMaxSize()) {
        InteractiveMap(
            modifier = Modifier.fillMaxSize(),
            currentLocation = journeyState.currentLocation,
            destination = journeyState.destination,
            thresholdMeters = journeyState.config.thresholdMeters,
            routeData = journeyState.routeData,
            isNavigating = true,
            language = language,
            onMapTap = { _, _ -> },
            onSelectDestination = {}
        )

        if (journeyState.status == JourneyStatus.ALARM_SNOOZED) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 70.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Snooze,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.BANGLA) "অ্যালার্ম স্নুজ করা হয়েছে" else "Alarm Snoozed",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = if (language == AppLanguage.BANGLA) "৩ মিনিট পর আবার বাজবে" else "Will ring again in 3 mins",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    Button(
                        onClick = onDismissAlarm,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "চালু করুন" else "Resume",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 0.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 5.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val (gpsText, gpsColor) = when (journeyState.gpsQuality) {
                    GpsSignalQuality.EXCELLENT -> (if (language == AppLanguage.BANGLA) "GPS চমৎকার" else "GPS Excellent") to Color(0xFF1B873F)
                    GpsSignalQuality.GOOD -> (if (language == AppLanguage.BANGLA) "GPS ভালো" else "GPS Good") to Color(0xFF1B873F)
                    GpsSignalQuality.MODERATE -> (if (language == AppLanguage.BANGLA) "GPS মাঝারি" else "GPS Moderate") to Color(0xFFE5A100)
                    GpsSignalQuality.WEAK -> (if (language == AppLanguage.BANGLA) "GPS দুর্বল" else "GPS Weak") to Color(0xFFD32F2F)
                    GpsSignalQuality.LOST -> (if (language == AppLanguage.BANGLA) "GPS বিচ্ছিন্ন" else "GPS Lost") to Color(0xFFD32F2F)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = gpsColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (journeyState.gpsQuality != GpsSignalQuality.LOST) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                            contentDescription = null,
                            tint = gpsColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = gpsText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = gpsColor
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = destName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "বাকি মোট দূরত্ব" else "Remaining Distance",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = GeoUtils.formatDistance(journeyState.remainingDistanceMeters, language),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "অ্যালার্ম বাজবে আর" else "Distance to Alert",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = GeoUtils.formatDistance(journeyState.distanceToThresholdMeters, language),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val speed = journeyState.currentLocation?.speedKmh ?: 0f
                    val speedText = if (language == AppLanguage.BANGLA) {
                        "${GeoUtils.toBengaliDigits(String.format("%.0f", speed))} কিমি/ঘ"
                    } else {
                        "${String.format("%.0f", speed)} km/h"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (language == AppLanguage.BANGLA) "গতি:" else "Speed:"} $speedText",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    val etaText = GeoUtils.formatDuration(journeyState.estimatedArrivalSeconds, language)
                    Text(
                        text = "${if (language == AppLanguage.BANGLA) "পৌঁছাতে বাকি:" else "ETA:"} $etaText",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (journeyState.isSimulated) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onFastForwardSimulator,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fast_forward_simulator_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "সিমুলেশন: সরাসরি অ্যালার্ম সীমানায় যান" else "Fast-forward: Jump into alert zone"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onEndJourney,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("end_journey_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.BANGLA) "যাত্রা সমাপ্ত করুন" else "End Journey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
