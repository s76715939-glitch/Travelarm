package com.example.ui.screens

import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.example.BuildConfig
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlarmConfiguration
import com.example.data.model.AppLanguage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    language: AppLanguage,
    alarmConfig: AlarmConfiguration,
    isTestingAlarm: Boolean,
    isBatteryOptimizationIgnored: Boolean = false,
    systemSounds: List<Pair<String, String>> = emptyList(),
    onLanguageChange: (AppLanguage) -> Unit,
    onAlarmConfigChange: (AlarmConfiguration) -> Unit,
    onTestAlarm: () -> Unit,
    onStopTestAlarm: () -> Unit,
    onRequestBatteryOptimization: () -> Unit = {},
    isCheckingUpdate: Boolean = false,
    onCheckForUpdates: () -> Unit = {}
) {
    val context = LocalContext.current

    val availableSounds = remember(systemSounds) {
        if (systemSounds.isNotEmpty()) {
            systemSounds
        } else {
            listOf(
                Pair("Default Alarm", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString() ?: ""),
                Pair("Default Ringtone", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.toString() ?: ""),
                Pair("Default Notification", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString() ?: "")
            )
        }
    }

    var ringtoneExpanded by remember { mutableStateOf(false) }

    val currentSelectedTitle = when {
        alarmConfig.selectedAlarmSound == "CUSTOM" -> {
            if (alarmConfig.customAlarmPath.isNotBlank()) {
                File(alarmConfig.customAlarmPath).name
            } else {
                if (language == AppLanguage.BANGLA) "কাস্টম MP3 ফাইল" else "Custom MP3"
            }
        }
        else -> {
            availableSounds.find { it.second == alarmConfig.selectedAlarmSound }?.first
                ?: availableSounds.firstOrNull()?.first
                ?: (if (language == AppLanguage.BANGLA) "ডিফল্ট অ্যালার্ম" else "Default Alarm")
        }
    }

    val customAlarmPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val customFile = File(context.filesDir, "custom_alarm.mp3")
                inputStream?.use { input ->
                    customFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                onAlarmConfigChange(
                    alarmConfig.copy(
                        selectedAlarmSound = "CUSTOM",
                        customAlarmPath = customFile.absolutePath
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("SettingsScreen", "Error saving custom alarm", e)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .padding(bottom = 80.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (language == AppLanguage.BANGLA) "সেটিংস" else "Settings & Preferences",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (language == AppLanguage.BANGLA) "ভাষা, অ্যালার্ম টিউন ও ব্যাকগ্রাউন্ড ট্র্যাকিং কনফিগার করুন" else "Configure language, alarm and background execution",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Language Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.BANGLA) "ভাষা নির্বাচন (Language)" else "App Language",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = language == AppLanguage.BANGLA,
                        onClick = { onLanguageChange(AppLanguage.BANGLA) }
                    )
                    Text(
                        text = "বাংলা (Bangla - ডিফল্ট)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (language == AppLanguage.BANGLA) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = language == AppLanguage.ENGLISH,
                        onClick = { onLanguageChange(AppLanguage.ENGLISH) }
                    )
                    Text(
                        text = "English",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (language == AppLanguage.ENGLISH) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Alarm Media & Style Settings Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.BANGLA) "অ্যালার্ম আউটপুট ও মাধ্যম" else "Alarm Output & Media",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = if (language == AppLanguage.BANGLA) "অ্যালার্ম সাউন্ড" else "Alarm Sound", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = if (language == AppLanguage.BANGLA) "সাইলেন্ট মোডেও রিং করবে" else "Rings even in Silent mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = alarmConfig.soundEnabled,
                        onCheckedChange = { onAlarmConfigChange(alarmConfig.copy(soundEnabled = it)) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = if (language == AppLanguage.BANGLA) "ভাইব্রেশন / কম্পন" else "Vibration", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = if (language == AppLanguage.BANGLA) "তীব্র ছন্দময় কম্পন" else "Intense repeating pulses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = alarmConfig.vibrationEnabled,
                        onCheckedChange = { onAlarmConfigChange(alarmConfig.copy(vibrationEnabled = it)) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = if (language == AppLanguage.BANGLA) "বাংলা ভয়েস অ্যানাউন্সমেন্ট" else "Voice Announcement", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = if (language == AppLanguage.BANGLA) "গন্তব্য ও সতর্কতার কথা বলবে" else "Announces arrival via voice", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = alarmConfig.voiceEnabled,
                        onCheckedChange = { onAlarmConfigChange(alarmConfig.copy(voiceEnabled = it)) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationImportant, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = if (language == AppLanguage.BANGLA) "সর্বোচ্চ ভলিউম (Max Volume)" else "Maximum Volume", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = if (language == AppLanguage.BANGLA) "পাবলিক বাসের কোলাহল ভেদ করার জন্য" else "Overcomes loud transit noise", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = alarmConfig.maxVolume,
                        onCheckedChange = { onAlarmConfigChange(alarmConfig.copy(maxVolume = it)) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Text(
                    text = if (language == AppLanguage.BANGLA) "সিস্টেম রিংটোন ও অ্যালার্ম টিউন ড্রপডাউন" else "System Ringtone & Alarm Tune Dropdown",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (language == AppLanguage.BANGLA) "ড্রপডাউন থেকে যেকোনো সিস্টেম রিংটোন বা কাস্টম MP3 সিলেক্ট করুন:" else "Select any system ringtone or custom MP3 from dropdown:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = ringtoneExpanded,
                    onExpandedChange = { ringtoneExpanded = !ringtoneExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentSelectedTitle,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ringtoneExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("alarm_sound_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = ringtoneExpanded,
                        onDismissRequest = { ringtoneExpanded = false }
                    ) {
                        availableSounds.forEach { (title, uriString) ->
                            DropdownMenuItem(
                                text = { Text(title) },
                                onClick = {
                                    onAlarmConfigChange(alarmConfig.copy(selectedAlarmSound = uriString))
                                    ringtoneExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (language == AppLanguage.BANGLA) "📂 কাস্টম MP3 ফাইল (Custom MP3)" else "📂 Custom MP3 File") },
                            onClick = {
                                ringtoneExpanded = false
                                customAlarmPicker.launch(arrayOf("audio/*"))
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Emergency Alarm Test Option
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.BANGLA) "জরুরি অ্যালার্ম টেস্ট করুন" else "Emergency Alarm Test",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (language == AppLanguage.BANGLA) "আপনার ফোনের স্পিকার, ভাইব্রেশন ও বাংলা ভয়েস ঠিকমতো বাজছে কিনা ৫ সেকেন্ডের জন্য পরীক্ষা করে দেখুন।" else "Verify that sound, vibration, and speech work correctly on your device for 5 seconds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (isTestingAlarm) onStopTestAlarm() else onTestAlarm()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("test_alarm_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTestingAlarm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(if (isTestingAlarm) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTestingAlarm) {
                            if (language == AppLanguage.BANGLA) "অ্যালার্ম পরীক্ষা বন্ধ করুন" else "Stop Test Alarm"
                        } else {
                            if (language == AppLanguage.BANGLA) "টেস্ট অ্যালার্ম বাজান" else "Play Test Alarm"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Battery Optimization Section with Action Button
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = if (isBatteryOptimizationIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "ব্যাটারি অপ্টিমাইজেশন" else "Battery Optimization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isBatteryOptimizationIgnored) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    ) {
                        Text(
                            text = if (isBatteryOptimizationIgnored) {
                                if (language == AppLanguage.BANGLA) "✓ নিষ্ক্রিয় (সঠিক)" else "✓ Unrestricted"
                            } else {
                                if (language == AppLanguage.BANGLA) "⚠️ অপ্টিমাইজেশন চালু" else "⚠️ Restricted"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isBatteryOptimizationIgnored) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (language == AppLanguage.BANGLA) {
                        "• স্ক্রিন লক থাকলেও Travelarm ফোরগ্রাউন্ড সার্ভিসের মাধ্যমে ব্যাকগ্রাউন্ডে লোকেশন ট্র্যাক করে যাতে আপনি সময়মতো জেগে উঠতে পারেন。\n" +
                        "• স্যামসাং, শাওমি (MIUI) ও অপো ফোনে অ্যান্ড্রয়েড যেন ব্যাকগ্রাউন্ড প্রসেস বন্ধ না করে, সেজন্য অ্যাপটিকে 'Unrestricted' বা অপ্টিমাইজেশন ছাড় দিন।"
                    } else {
                        "• Travelarm uses an ongoing foreground service to track your journey while phone is locked so you wake up on time.\n" +
                        "• To prevent Android or OEM power savers (Samsung, Xiaomi, etc.) from sleeping background GPS, set battery optimization to 'Unrestricted'."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onRequestBatteryOptimization,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("battery_optimization_action_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBatteryOptimizationIgnored) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (isBatteryOptimizationIgnored) Icons.Default.CheckCircle else Icons.Default.FlashOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBatteryOptimizationIgnored) {
                            if (language == AppLanguage.BANGLA) "ব্যাটারি সেটিংস পর্যালোচনা করুন" else "Review Battery Settings"
                        } else {
                            if (language == AppLanguage.BANGLA) "ব্যাটারি অপ্টিমাইজেশন ঠিক করুন" else "Disable Battery Optimization"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. App Update Section
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "অ্যাপ আপডেট (App Updates)" else "App Updates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (language == AppLanguage.BANGLA)
                        "বর্তমান সংস্করণ: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})। নতুন কোনো রিলিজ এসেছে কিনা এখনই পরীক্ষা করুন।"
                    else
                        "Current version: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}). Check if a new release is available on GitHub.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onCheckForUpdates,
                    enabled = !isCheckingUpdate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("check_updates_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "আপডেট খোঁজা হচ্ছে..." else "Checking for Updates...",
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "আপডেট চেক করুন" else "Check for Updates",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.travelarm_logo),
                    contentDescription = "Travelarm Logo",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Travelarm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language == AppLanguage.BANGLA)
                            "স্মার্ট ট্রাভেল অ্যালার্ম • সংস্করণ ${com.example.engine.GeoUtils.toBengaliDigits(BuildConfig.VERSION_NAME)}"
                        else
                            "Smart Travel Alarm • Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
