package com.sstechzone.travelarm.engine

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
import com.sstechzone.travelarm.data.model.AlarmConfiguration
import com.sstechzone.travelarm.data.model.AppLanguage
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.util.Locale

/**
 * Robust Alarm Engine:
 * - Plays alarm sound and voice MP3 simultaneously at 100% full volume.
 * - Enforces maximum volume on STREAM_ALARM, STREAM_MUSIC, and STREAM_RING.
 * - Uses AudioAttributes.FLAG_AUDIBILITY_ENFORCED and USAGE_ALARM to bypass silent/vibrate/DND modes.
 * - Handles vibration and fallback text-to-speech gracefully.
 */
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

    /**
     * Enforces maximum volume across all critical audio streams (Alarm, Music, Ringtone)
     * and switches out of silent/vibrate mode to guarantee audibility.
     */
    private fun enforceMaxAudioVolumes() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            // Attempt to restore normal ringer mode if muted or vibrating
            try {
                if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
            } catch (e: Exception) {
                Log.w("AlarmEngine", "Could not adjust ringer mode (DND policy restriction)", e)
            }

            // Maximize STREAM_ALARM
            val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0)

            // Maximize STREAM_MUSIC (used by media audio & voice tracks)
            val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)

            // Maximize STREAM_RING
            val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRing, 0)
        } catch (e: Exception) {
            Log.w("AlarmEngine", "Could not set max volume on audio streams", e)
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

        // Enforce maximum volume on device for emergency waking
        enforceMaxAudioVolumes()

        // 1. Audio Playback (Alarm Sound)
        if (config.soundEnabled) {
            startSound(config)
        }

        // 2. Voice Audio Playback (Plays simultaneously with alarm sound)
        if (config.voiceEnabled) {
            startVoice(language, destinationName)
        }

        // 3. Vibration
        if (config.vibrationEnabled) {
            startVibration()
        }
    }

    private fun buildAudibleAlarmAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()
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

            var playedWithMediaPlayer = false
            try {
                if (uri != null) {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(context, uri)
                        setAudioAttributes(buildAudibleAlarmAttributes())
                        isLooping = true
                        setVolume(1.0f, 1.0f) // 100% full volume
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
                            audioAttributes = buildAudibleAlarmAttributes()
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            isLooping = true
                            volume = 1.0f
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
                            setAudioAttributes(buildAudibleAlarmAttributes())
                            isLooping = true
                            setVolume(1.0f, 1.0f)
                            prepare()
                            start()
                        }
                    } catch (_: Exception) {
                        ringtone = RingtoneManager.getRingtone(context, fallbackUri)?.apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                audioAttributes = buildAudibleAlarmAttributes()
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                isLooping = true
                                volume = 1.0f
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
                setAudioAttributes(buildAudibleAlarmAttributes())
                isLooping = false
                setVolume(1.0f, 1.0f) // 100% full volume for voice announcement
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
