package com.sstechzone.travelarm.data.repository

import com.sstechzone.travelarm.data.local.JourneyDao
import com.sstechzone.travelarm.data.local.JourneyEntity
import com.sstechzone.travelarm.data.local.PreferencesManager
import com.sstechzone.travelarm.data.model.AlarmConfiguration
import com.sstechzone.travelarm.data.model.AppLanguage
import com.sstechzone.travelarm.data.model.MapType
import kotlinx.coroutines.flow.Flow

class JourneyRepository(
    private val journeyDao: JourneyDao,
    private val preferencesManager: PreferencesManager
) {
    val allJourneys: Flow<List<JourneyEntity>> = journeyDao.getAllJourneys()
    val recentJourneys: Flow<List<JourneyEntity>> = journeyDao.getRecentJourneys()

    val alarmConfig: Flow<AlarmConfiguration> = preferencesManager.alarmConfigFlow
    val currentLanguage: Flow<AppLanguage> = preferencesManager.languageFlow
    val mapType: Flow<MapType> = preferencesManager.mapTypeFlow
    val isSimulatorEnabled: Flow<Boolean> = preferencesManager.simulatorEnabledFlow
    val isOnboardingCompleted: Flow<Boolean> = preferencesManager.onboardingCompletedFlow

    suspend fun saveCompletedJourney(journey: JourneyEntity): Long {
        return journeyDao.insertJourney(journey)
    }

    suspend fun deleteJourney(id: Long) {
        journeyDao.deleteJourneyById(id)
    }

    suspend fun clearHistory() {
        journeyDao.clearAll()
    }

    suspend fun updateAlarmConfig(config: AlarmConfiguration) {
        preferencesManager.updateAlarmConfig(config)
    }

    suspend fun updateThreshold(meters: Int) {
        preferencesManager.updateThreshold(meters)
    }

    suspend fun setLanguage(language: AppLanguage) {
        preferencesManager.setLanguage(language)
    }

    suspend fun setMapType(mapType: MapType) {
        preferencesManager.setMapType(mapType)
    }

    suspend fun setSimulatorEnabled(enabled: Boolean) {
        preferencesManager.setSimulatorEnabled(enabled)
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        preferencesManager.setOnboardingCompleted(completed)
    }
}
