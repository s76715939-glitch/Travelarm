package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.AppLanguage
import com.example.data.model.UpdateStatus
import com.example.engine.GeoUtils

@Composable
fun InAppUpdateDialog(
    status: UpdateStatus,
    language: AppLanguage,
    onStartDownload: (String) -> Unit,
    onInstall: (java.io.File) -> Unit,
    onDismiss: () -> Unit,
    onRetryCheck: () -> Unit = {}
) {
    when (status) {
        is UpdateStatus.UpdateAvailable -> {
            val info = status.info
            AlertDialog(
                onDismissRequest = {
                    if (!info.forceUpdate) onDismiss()
                },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "নতুন সংস্করণ উপলব্ধ!" else "New Update Available!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BANGLA)
                                        "ভার্সন: v${info.latestVersionName} (Build ${info.latestVersionCode})"
                                    else
                                        "Version: v${info.latestVersionName} (Build ${info.latestVersionCode})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "বর্তমান: v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (language == AppLanguage.BANGLA) "নতুন কী পরিবর্তন এসেছে:" else "What's new:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = info.releaseNotes,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onStartDownload(info.apkUrl) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("update_now_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (language == AppLanguage.BANGLA) "আপডেট করুন" else "Update Now")
                    }
                },
                dismissButton = {
                    if (!info.forceUpdate) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (language == AppLanguage.BANGLA) "পরে" else "Later")
                        }
                    }
                }
            )
        }

        is UpdateStatus.Downloading -> {
            AlertDialog(
                onDismissRequest = { /* Prevent dismiss while downloading */ },
                icon = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "আপডেট ডাউনলোড হচ্ছে..." else "Downloading Update...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (status.progressPercent >= 0) {
                            LinearProgressIndicator(
                                progress = { status.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            val percentText = if (language == AppLanguage.BANGLA)
                                "${GeoUtils.toBengaliDigits(status.progressPercent.toString())}%"
                            else
                                "${status.progressPercent}%"
                            Text(
                                text = percentText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA)
                                "অনুগ্রহ করে অপেক্ষা করুন, ডাউনলোড শেষ হলে ইনস্টল প্রক্রিয়া শুরু হবে।"
                            else
                                "Please wait, installation will begin once download completes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {}
            )
        }

        is UpdateStatus.DownloadCompleted -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "ডাউনলোড সম্পন্ন!" else "Download Complete!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (language == AppLanguage.BANGLA)
                            "নতুন APK ফাইলটি তৈরি হয়েছে। আপনার অ্যাপ আপডেট করতে 'ইনস্টল করুন' বাটনে চাপুন।"
                        else
                            "The update APK is ready. Tap 'Install Now' to finish updating your app.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onInstall(status.apkFile) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (language == AppLanguage.BANGLA) "ইনস্টল করুন" else "Install Now")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text(if (language == AppLanguage.BANGLA) "বন্ধ করুন" else "Close")
                    }
                }
            )
        }

        is UpdateStatus.UpToDate -> {
            if (status.checkedManually) {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    icon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "অ্যাপ আপ-টু-ডেট আছে" else "App is Up to Date",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = if (language == AppLanguage.BANGLA)
                                "আপনি Travelarm এর সর্বশেষ সংস্করণ (v${BuildConfig.VERSION_NAME}) ব্যবহার করছেন।"
                            else
                                "You are using the latest version of Travelarm (v${BuildConfig.VERSION_NAME}).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                            Text(if (language == AppLanguage.BANGLA) "ঠিক আছে" else "OK")
                        }
                    }
                )
            }
        }

        is UpdateStatus.Error -> {
            if (status.checkedManually) {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    icon = {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "আপডেট চেক ব্যর্থ" else "Update Check Failed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(text = status.message, style = MaterialTheme.typography.bodyMedium)
                    },
                    confirmButton = {
                        Button(onClick = onRetryCheck, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == AppLanguage.BANGLA) "আবার চেষ্টা করুন" else "Retry")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                            Text(if (language == AppLanguage.BANGLA) "বন্ধ করুন" else "Dismiss")
                        }
                    }
                )
            }
        }

        is UpdateStatus.Checking -> {
            AlertDialog(
                onDismissRequest = { /* wait */ },
                title = {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "আপডেট চেক করা হচ্ছে..." else "Checking for updates...",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (language == AppLanguage.BANGLA) "অনুগ্রহ করে অপেক্ষা করুন..." else "Please wait...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {}
            )
        }

        UpdateStatus.Idle -> { /* do nothing */ }
    }
}
