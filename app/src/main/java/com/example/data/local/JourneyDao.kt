package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyDao {
    @Query("SELECT * FROM journey_history ORDER BY startTimeMillis DESC")
    fun getAllJourneys(): Flow<List<JourneyEntity>>

    @Query("SELECT * FROM journey_history ORDER BY startTimeMillis DESC LIMIT 5")
    fun getRecentJourneys(): Flow<List<JourneyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourney(journey: JourneyEntity): Long

    @Query("DELETE FROM journey_history WHERE id = :id")
    suspend fun deleteJourneyById(id: Long)

    @Query("DELETE FROM journey_history")
    suspend fun clearAll()
}
