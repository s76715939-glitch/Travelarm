package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlarmConfiguration
import com.example.data.model.AppLanguage
import com.example.data.model.Destination
import com.example.data.model.LocationReading
import com.example.engine.GeoUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DestinationConfirmSheet(
    destination: Destination,
    currentLocation: LocationReading?,
    alarmConfig: AlarmConfiguration,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onThresholdChange: (Int) -> Unit,
    onConfigChange: (AlarmConfiguration) -> Unit,
    onStartJourney: (useSimulator: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var useSimulator by remember { mutableStateOf(false) }

    val rawDistance = remember(currentLocation, destination) {
        if (currentLocation != null) {
            GeoUtils.calculateGeodesicDistanceMeters(
                currentLocation.latitude, currentLocation.longitude,
                destination.latitude, destination.longitude
            )
        } else {
            15000.0 // Default reference distance
        }
    }

    val estimatedEta = remember(rawDistance) {
        GeoUtils.estimateDurationSeconds(rawDistance, 35f)
    }

    val presetThresholds = listOf(200, 500, 1000, 2000)
    var customThresholdSlider by remember { mutableFloatStateOf(alarmConfig.thresholdMeters.toFloat()) }
    var isCustomSelected by remember { mutableStateOf(!presetThresholds.contains(alarmConfig.thresholdMeters)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) destination.nameBn else destination.nameEn,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language == AppLanguage.BANGLA) destination.addressBn else destination.addressEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (language == AppLanguage.BANGLA) "সোজা রৈখিক দূরত্ব (Geodesic)" else "Straight-line Distance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = GeoUtils.formatDistance(rawDistance, language),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (language == AppLanguage.BANGLA) "আনুমানিক সময় (~৩৫ কিমি/ঘ)" else "Estimated Time (~35 km/h)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = GeoUtils.formatDuration(estimatedEta, language),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "সড়ক পথের দূরত্ব ট্রাফিকের ওপর নির্ভরশীল" else "Road route distance depends on actual bus routing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (language == AppLanguage.BANGLA) "কত দূরত্ব আগে অ্যালার্ম বাজবে?" else "Alert distance before destination:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetThresholds.forEach { meters ->
                    val isSelected = !isCustomSelected && alarmConfig.thresholdMeters == meters
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            isCustomSelected = false
                            onThresholdChange(meters)
                        },
                        label = {
                            Text(
                                text = if (meters >= 1000) {
                                    if (language == AppLanguage.BANGLA) "${GeoUtils.toBengaliDigits((meters / 1000).toString())} কিমি আগে" else "${meters / 1000} km before"
                                } else {
                                    if (language == AppLanguage.BANGLA) "${GeoUtils.toBengaliDigits(meters.toString())} মি আগে" else "${meters} m before"
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("threshold_chip_$meters")
                    )
                }

                FilterChip(
                    selected = isCustomSelected,
                    onClick = { isCustomSelected = true },
                    label = { Text(if (language == AppLanguage.BANGLA) "কাস্টম" else "Custom") }
                )
            }

            if (isCustomSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        text = if (language == AppLanguage.BANGLA)
                            "কাস্টম দূরত্ব: ${GeoUtils.toBengaliDigits(customThresholdSlider.toInt().toString())} মিটার"
                        else
                            "Custom distance: ${customThresholdSlider.toInt()} meters",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = customThresholdSlider,
                        onValueChange = {
                            customThresholdSlider = it
                            onThresholdChange(it.toInt())
                        },
                        valueRange = 0f..5000f
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (language == AppLanguage.BANGLA) "অ্যালার্মের ধরন ও মাধ্যম" else "Alert Styles:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "উচ্চ শব্দে অ্যালার্ম রিংটোন" else "Loud Alarm Ringtone",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Checkbox(
                            checked = alarmConfig.soundEnabled,
                            onCheckedChange = { onConfigChange(alarmConfig.copy(soundEnabled = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "ধারাবাহিক ভাইব্রেশন / কম্পন" else "Repeating Vibration",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Checkbox(
                            checked = alarmConfig.vibrationEnabled,
                            onCheckedChange = { onConfigChange(alarmConfig.copy(vibrationEnabled = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "ভয়েস ঘোষণা" else "Voice Announcement",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Checkbox(
                            checked = alarmConfig.voiceEnabled,
                            onCheckedChange = { onConfigChange(alarmConfig.copy(voiceEnabled = it)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "জার্নি সিমুলেটর মোড (টেস্টিং)" else "Journey Simulator Mode (Testing)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (language == AppLanguage.BANGLA) "বাসে না চড়েই ভার্চুয়াল মুভমেন্ট ও অ্যালার্ম পরীক্ষা করুন" else "Test virtual travel towards destination and alarm triggers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useSimulator,
                        onCheckedChange = { useSimulator = it },
                        modifier = Modifier.testTag("simulator_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onStartJourney(useSimulator) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start_journey_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == AppLanguage.BANGLA) "যাত্রা শুরু করুন" else "Start Journey",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
