package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AlarmConfiguration
import com.example.data.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "travelarm_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_THRESHOLD_METERS = intPreferencesKey("threshold_meters")
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val KEY_VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val KEY_MAX_VOLUME = booleanPreferencesKey("max_volume")
        val KEY_ALARM_SOUND = stringPreferencesKey("alarm_sound_type")
        val KEY_CUSTOM_ALARM_PATH = stringPreferencesKey("custom_alarm_path")
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_SIMULATOR_ENABLED = booleanPreferencesKey("simulator_enabled")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val alarmConfigFlow: Flow<AlarmConfiguration> = context.dataStore.data.map { prefs ->
        AlarmConfiguration(
            soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[KEY_VIBRATION_ENABLED] ?: true,
            voiceEnabled = prefs[KEY_VOICE_ENABLED] ?: true,
            thresholdMeters = prefs[KEY_THRESHOLD_METERS] ?: 1000,
            maxVolume = prefs[KEY_MAX_VOLUME] ?: true,
            selectedAlarmSound = prefs[KEY_ALARM_SOUND] ?: "DEFAULT_ALARM",
            customAlarmPath = prefs[KEY_CUSTOM_ALARM_PATH] ?: ""
        )
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        val langStr = prefs[KEY_LANGUAGE] ?: AppLanguage.BANGLA.name
        try {
            AppLanguage.valueOf(langStr)
        } catch (_: Exception) {
            AppLanguage.BANGLA
        }
    }

    val simulatorEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SIMULATOR_ENABLED] ?: false
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun updateThreshold(meters: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THRESHOLD_METERS] = meters
        }
    }

    suspend fun updateAlarmConfig(config: AlarmConfiguration) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SOUND_ENABLED] = config.soundEnabled
            prefs[KEY_VIBRATION_ENABLED] = config.vibrationEnabled
            prefs[KEY_VOICE_ENABLED] = config.voiceEnabled
            prefs[KEY_THRESHOLD_METERS] = config.thresholdMeters
            prefs[KEY_MAX_VOLUME] = config.maxVolume
            prefs[KEY_ALARM_SOUND] = config.selectedAlarmSound
            prefs[KEY_CUSTOM_ALARM_PATH] = config.customAlarmPath
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language.name
        }
    }

    suspend fun setSimulatorEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SIMULATOR_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }
}
