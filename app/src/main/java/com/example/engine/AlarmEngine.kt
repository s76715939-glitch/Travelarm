package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.data.model.AlarmConfiguration
import com.example.data.model.AppLanguage
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.util.Locale

class AlarmEngine(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var voicePlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isAlarmActive = false

    init {
        initVibrator()
        initTts()
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.forLanguageTag("bn-BD"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.ENGLISH)
                    }
                    isTtsReady = true
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmEngine", "Error initializing TTS", e)
        }
    }

    fun startAlarm(
        scope: CoroutineScope,
        config: AlarmConfiguration,
        language: AppLanguage = AppLanguage.BANGLA,
        destinationName: String = ""
    ) {
        if (isAlarmActive) return
        isAlarmActive = true

        // 1. Audio Playback (Alarm Sound)
        if (config.soundEnabled) {
            startSound(config)
        }

        // 2. Vibration
        if (config.vibrationEnabled) {
            startVibration()
        }

        // 3. Voice Audio Playback
        if (config.voiceEnabled) {
            startVoice(language, destinationName)
        }
    }

    private fun startSound(config: AlarmConfiguration) {
        try {
            stopSoundPlayback()

            val uri = when {
                config.selectedAlarmSound == "CUSTOM" -> {
                    if (config.customAlarmPath.isNotBlank() && File(config.customAlarmPath).exists()) {
                        Uri.fromFile(File(config.customAlarmPath))
                    } else {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    }
                }
                config.selectedAlarmSound.isNotBlank() && (config.selectedAlarmSound.startsWith("content://") || config.selectedAlarmSound.startsWith("file://")) -> {
                    Uri.parse(config.selectedAlarmSound)
                }
                else -> {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
            }

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (config.maxVolume && audioManager != null) {
                try {
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
                } catch (e: Exception) {
                    Log.w("AlarmEngine", "Could not set max alarm volume", e)
                }
            }

            var playedWithMediaPlayer = false
            try {
                if (uri != null) {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(context, uri)
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        isLooping = true
                        prepare()
                        start()
                    }
                    playedWithMediaPlayer = true
                }
            } catch (mpEx: Exception) {
                Log.w("AlarmEngine", "MediaPlayer failed for $uri, trying RingtoneManager", mpEx)
                stopSoundPlayback()
            }

            if (!playedWithMediaPlayer && uri != null) {
                try {
                    ringtone = RingtoneManager.getRingtone(context, uri)?.apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            audioAttributes = AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            isLooping = true
                        }
                        play()
                    }
                } catch (rEx: Exception) {
                    Log.w("AlarmEngine", "RingtoneManager failed for $uri", rEx)
                }
            }

            if (!playedWithMediaPlayer && (ringtone == null || ringtone?.isPlaying == false)) {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                if (fallbackUri != null) {
                    try {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(context, fallbackUri)
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                            isLooping = true
                            prepare()
                            start()
                        }
                    } catch (_: Exception) {
                        ringtone = RingtoneManager.getRingtone(context, fallbackUri)?.apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                audioAttributes = AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                isLooping = true
                            }
                            play()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmEngine", "Error playing sound", e)
        }
    }

    private fun startVoice(language: AppLanguage, destinationName: String) {
        var playedAsset = false
        try {
            voicePlayer?.release()
            val assetPath = if (language == AppLanguage.BANGLA) "voice/Bangla.mp3" else "voice/English.mp3"
            val afd = context.assets.openFd(assetPath)

            voicePlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = false
                setOnCompletionListener {
                    if (isAlarmActive) {
                        try {
                            seekTo(0)
                            start()
                        } catch (_: Exception) {}
                    }
                }
                prepare()
                afd.close()
                start()
            }
            playedAsset = true
        } catch (e: Exception) {
            Log.w("AlarmEngine", "Voice asset not found or failed, using Android TextToSpeech fallback", e)
        }

        // Bulletproof TTS fallback
        if (!playedAsset && isTtsReady) {
            try {
                val text = if (language == AppLanguage.BANGLA) {
                    val name = if (destinationName.isNotBlank()) "$destinationName " else ""
                    "সতর্কতা! আপনার গন্তব্য ${name}কাছাকাছি। ঘুম থেকে উঠুন এবং নামার জন্য প্রস্তুত হন।"
                } else {
                    val name = if (destinationName.isNotBlank()) "to $destinationName " else ""
                    "Alert! You are approaching ${name}destination. Please wake up and prepare to get off."
                }
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AlarmAnnouncement")
            } catch (ex: Exception) {
                Log.e("AlarmEngine", "TTS speak failed", ex)
            }
        }
    }

    private fun startVibration() {
        try {
            val pattern = longArrayOf(0, 600, 300, 600, 300, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("AlarmEngine", "Error starting vibration", e)
        }
    }

    private fun stopSoundPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}

        try {
            ringtone?.stop()
            ringtone = null
        } catch (_: Exception) {}
    }

    fun stopAlarm() {
        isAlarmActive = false
        stopSoundPlayback()

        try {
            voicePlayer?.stop()
            voicePlayer?.release()
            voicePlayer = null
        } catch (e: Exception) {
            Log.e("AlarmEngine", "Error stopping voicePlayer", e)
        }

        try {
            tts?.stop()
        } catch (_: Exception) {}

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("AlarmEngine", "Error cancelling vibrator", e)
        }
    }

    fun release() {
        stopAlarm()
        try {
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}
    }
}
