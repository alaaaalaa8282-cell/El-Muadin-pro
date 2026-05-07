package com.AbuAlaa.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.AbuAlaa.data.PrayerDay

/**
 * Room Entity for storing prayer times locally
 * Uses the date as the primary key to avoid duplicates
 */
@Entity(tableName = "prayer_times")
data class PrayerDayEntity(
    @PrimaryKey
    val dateKey: String, // Format: yyyy-MM-dd (e.g., 2026-01-23)
    
    val imsak: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val timezone: String,
    val gregorianDate: String,
    val hijriDate: String,
    val hijriMonthNumber: Int,
    
    // Timestamp for cache invalidation
    val cachedAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert Entity to Domain Model
     */
    fun toPrayerDay(): PrayerDay {
        return PrayerDay(
            imsak = imsak,
            fajr = fajr,
            sunrise = sunrise,
            dhuhr = dhuhr,
            asr = asr,
            maghrib = maghrib,
            isha = isha,
            timezone = timezone,
            gregorianDate = gregorianDate,
            hijriDate = hijriDate,
            hijriMonthNumber = hijriMonthNumber
        )
    }

    companion object {
        /**
         * Create Entity from Domain Model
         */
        fun fromPrayerDay(dateKey: String, prayerDay: PrayerDay): PrayerDayEntity {
            return PrayerDayEntity(
                dateKey = dateKey,
                imsak = prayerDay.imsak,
                fajr = prayerDay.fajr,
                sunrise = prayerDay.sunrise,
                dhuhr = prayerDay.dhuhr,
                asr = prayerDay.asr,
                maghrib = prayerDay.maghrib,
                isha = prayerDay.isha,
                timezone = prayerDay.timezone,
                gregorianDate = prayerDay.gregorianDate,
                hijriDate = prayerDay.hijriDate,
                hijriMonthNumber = prayerDay.hijriMonthNumber
            )
        }
    }
}
