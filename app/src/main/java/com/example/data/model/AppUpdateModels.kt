package com.example.data.model

import java.io.File

data class AppUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val apkUrl: String,
    val releaseNotesBn: String,
    val releaseNotesEn: String,
    val forceUpdate: Boolean = false
) {
    fun getReleaseNotes(language: AppLanguage): String {
        return if (language == AppLanguage.BANGLA) {
            releaseNotesBn.ifBlank { releaseNotesEn }
        } else {
            releaseNotesEn.ifBlank { releaseNotesBn }
        }
    }
}

sealed interface UpdateStatus {
    object Idle : UpdateStatus
    object Checking : UpdateStatus
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateStatus
    data class UpToDate(val checkedManually: Boolean = false) : UpdateStatus
    data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateStatus
    data class DownloadCompleted(val apkFile: File) : UpdateStatus
    data class Error(val message: String, val checkedManually: Boolean = false) : UpdateStatus
}
