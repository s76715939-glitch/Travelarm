package com.sstechzone.travelarm.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.sstechzone.travelarm.BuildConfig
import com.sstechzone.travelarm.data.model.AppUpdateInfo
import com.sstechzone.travelarm.data.model.UpdateStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class UpdateManager(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "UpdateManager"
    }

    private fun loadConfig(): Pair<String, String> {
        var versionUrl = ""
        var apkUrl = ""
        try {
            val inputStream = context.assets.open("update_config.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            if (json.has("versionCheckUrl")) {
                versionUrl = json.getString("versionCheckUrl")
            }
            if (json.has("githubReleaseApkUrl")) {
                apkUrl = json.getString("githubReleaseApkUrl")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not load update_config.json from assets", e)
        }
        return Pair(versionUrl, apkUrl)
    }

    /**
     * Checks if a newer version of the app is available on GitHub.
     */
    suspend fun checkForUpdate(checkedManually: Boolean = false): UpdateStatus = withContext(Dispatchers.IO) {
        try {
            val (versionCheckUrl, defaultApkUrl) = loadConfig()
            if (versionCheckUrl.isBlank()) {
                return@withContext UpdateStatus.Error("আপডেট কনফিগারেশন লিঙ্ক পাওয়া যায়নি", checkedManually)
            }
            // Append timestamp query parameter to bypass GitHub raw CDN caching
            val urlWithTimestamp = "$versionCheckUrl?t=${System.currentTimeMillis()}"
            val request = Request.Builder()
                .url(urlWithTimestamp)
                .header("User-Agent", "Travelarm-Android/${BuildConfig.VERSION_NAME}")
                .header("Cache-Control", "no-cache")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Update check failed with HTTP ${response.code}")
                    return@withContext UpdateStatus.Error("সার্ভার থেকে আপডেটের তথ্য পাওয়া যায়নি (${response.code})", checkedManually)
                }

                val responseBody = response.body?.string() ?: return@withContext UpdateStatus.Error("ফাঁকা রেসপন্স পাওয়া গেছে", checkedManually)
                val json = JSONObject(responseBody)

                val latestVersionCode = json.optInt("latestVersionCode", BuildConfig.VERSION_CODE)
                val latestVersionName = json.optString("latestVersionName", BuildConfig.VERSION_NAME)
                val apkUrl = defaultApkUrl // Always use the URL from local config as requested
                val defaultNotes = json.optString("releaseNotes", "নতুন সংস্করণ উপলব্ধ। / New version available.")
                val releaseNotesBn = json.optString("releaseNotesBn", defaultNotes)
                val releaseNotesEn = json.optString("releaseNotesEn", defaultNotes)
                val forceUpdate = json.optBoolean("forceUpdate", false)

                val currentVersionCode = BuildConfig.VERSION_CODE
                Log.d(TAG, "Current versionCode: $currentVersionCode, Remote versionCode: $latestVersionCode")

                if (latestVersionCode > currentVersionCode && apkUrl.isNotBlank()) {
                    val updateInfo = AppUpdateInfo(
                        latestVersionCode = latestVersionCode,
                        latestVersionName = latestVersionName,
                        apkUrl = apkUrl,
                        releaseNotesBn = releaseNotesBn,
                        releaseNotesEn = releaseNotesEn,
                        forceUpdate = forceUpdate
                    )
                    UpdateStatus.UpdateAvailable(updateInfo)
                } else {
                    UpdateStatus.UpToDate(checkedManually)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception checking update", e)
            UpdateStatus.Error("আপডেট চেক করতে ব্যর্থ হয়েছে: ${e.localizedMessage ?: "নেটওয়ার্ক সমস্যা"}", checkedManually)
        }
    }

    /**
     * Downloads the APK file streaming byte by byte and emits download progress percentages.
     */
    fun downloadApk(apkUrl: String): Flow<UpdateStatus> = flow {
        emit(UpdateStatus.Downloading(0, 0L, 0L))
        try {
            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "Travelarm-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(UpdateStatus.Error("APK ডাউনলোড ব্যর্থ হয়েছে (${response.code})"))
                return@flow
            }

            val body = response.body ?: run {
                emit(UpdateStatus.Error("ডাউনলোড রেসপন্স পাওয়া যায়নি"))
                return@flow
            }

            val contentLength = body.contentLength()
            val destinationDir = context.getExternalFilesDir(null) ?: context.cacheDir
            val apkFile = File(destinationDir, "travelarm_update.apk")

            if (apkFile.exists()) {
                apkFile.delete()
            }

            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                inputStream = body.byteStream()
                outputStream = FileOutputStream(apkFile)

                val buffer = ByteArray(8192)
                var downloadedBytes = 0L
                var read: Int
                var lastReportedPercent = -1

                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                    downloadedBytes += read

                    if (contentLength > 0) {
                        val percent = ((downloadedBytes * 100) / contentLength).toInt().coerceIn(0, 100)
                        if (percent != lastReportedPercent) {
                            lastReportedPercent = percent
                            emit(UpdateStatus.Downloading(percent, downloadedBytes, contentLength))
                        }
                    } else {
                        emit(UpdateStatus.Downloading(-1, downloadedBytes, -1L))
                    }
                }
                outputStream.flush()
                emit(UpdateStatus.DownloadCompleted(apkFile))
            } finally {
                try { inputStream?.close() } catch (_: Exception) {}
                try { outputStream?.close() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK", e)
            emit(UpdateStatus.Error("ডাউনলোডে ত্রুটি: ${e.localizedMessage ?: "অজ্ঞাত সমস্যা"}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Prompts the Android OS Package Installer to install the downloaded APK.
     */
    fun installApk(apkFile: File): Boolean {
        if (!apkFile.exists()) {
            Log.e(TAG, "APK file does not exist at ${apkFile.absolutePath}")
            return false
        }

        return try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            // On Android 8.0+ check unknown sources permission if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    try {
                        val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(settingsIntent)
                    } catch (e: Exception) {
                        Log.w(TAG, "Cannot launch MANAGE_UNKNOWN_APP_SOURCES settings", e)
                    }
                }
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching package installer", e)
            false
        }
    }
}
