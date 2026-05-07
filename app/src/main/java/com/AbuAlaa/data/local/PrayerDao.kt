package com.AbuAlaa.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for prayer times
 */
@Dao
interface PrayerDao {
    
    /**
     * Insert or replace prayer times for a specific date
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerDay(prayerDay: PrayerDayEntity)
    
    /**
     * Insert multiple prayer days at once
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerDays(prayerDays: List<PrayerDayEntity>)
    
    /**
     * Get prayer times for a specific date
     */
    @Query("SELECT * FROM prayer_times WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getPrayerDay(dateKey: String): PrayerDayEntity?
    
    /**
     * Get prayer times for a range of dates (for week view)
     */
    @Query("SELECT * FROM prayer_times WHERE dateKey >= :startDate AND dateKey <= :endDate ORDER BY dateKey ASC")
    suspend fun getPrayerDaysInRange(startDate: String, endDate: String): List<PrayerDayEntity>
    
    /**
     * Get all cached prayer times as Flow for reactive updates
     */
    @Query("SELECT * FROM prayer_times ORDER BY dateKey ASC")
    fun getAllPrayerDaysFlow(): Flow<List<PrayerDayEntity>>
    
    /**
     * Delete old cached data (older than specified days)
     */
    @Query("DELETE FROM prayer_times WHERE dateKey < :beforeDate")
    suspend fun deleteOldData(beforeDate: String)
    
    /**
     * Clear all cached data
     */
    @Query("DELETE FROM prayer_times")
    suspend fun clearAll()
    
    /**
     * Check if we have data for today
     */
    @Query("SELECT COUNT(*) FROM prayer_times WHERE dateKey = :dateKey")
    suspend fun hasDataForDate(dateKey: String): Int
}
