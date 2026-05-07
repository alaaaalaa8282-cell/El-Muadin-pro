package com.AbuAlaa.data.repo

import android.util.Log
import com.AbuAlaa.data.CalculationMethod
import com.AbuAlaa.data.PrayerDay
import com.AbuAlaa.data.local.PrayerDao
import com.AbuAlaa.data.local.PrayerDayEntity
import com.AbuAlaa.data.remote.AladhanApi
import com.AbuAlaa.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerRepository @Inject constructor(
    private val api: AladhanApi,
    private val prayerDao: PrayerDao
) {
    companion object {
        private const val TAG = "PrayerRepository"
        private const val CACHE_DAYS = 7
    }

    /**
     * Get prayer times for a specific date with caching support
     * 1. Try to get from cache first
     * 2. If not in cache or expired, fetch from API and cache
     * 3. If API fails, return cached data if available
     */
    suspend fun getPrayerDayByCoordinates(
        dateDdMmYyyy: String,
        lat: Double,
        lon: Double,
        method: CalculationMethod
    ): PrayerDay {
        val dateKey = convertToDateKey(dateDdMmYyyy)
        
        // Try to get from cache first
        val cached = prayerDao.getPrayerDay(dateKey)
        if (cached != null && !isCacheExpired(cached)) {
            Log.d(TAG, "Returning cached data for $dateKey")
            return cached.toPrayerDay()
        }
        
        // Fetch from API
        return try {
            val res = api.timingsByCoordinates(
                date = dateDdMmYyyy,
                latitude = lat,
                longitude = lon,
                method = method.apiId
            )
            val prayerDay = res.toPrayerDay()
            
            // Cache the result
            prayerDao.insertPrayerDay(PrayerDayEntity.fromPrayerDay(dateKey, prayerDay))
            Log.d(TAG, "Cached new data for $dateKey")
            
            prayerDay
        } catch (e: Exception) {
            Log.e(TAG, "API error, trying cache: ${e.message}")
            // If API fails, return cached data if available
            cached?.toPrayerDay() ?: throw e
        }
    }

    suspend fun getPrayerDayByCity(
        dateDdMmYyyy: String,
        city: String,
        country: String,
        method: CalculationMethod
    ): PrayerDay {
        val dateKey = convertToDateKey(dateDdMmYyyy)
        
        // Try to get from cache first
        val cached = prayerDao.getPrayerDay(dateKey)
        if (cached != null && !isCacheExpired(cached)) {
            Log.d(TAG, "Returning cached data for $dateKey")
            return cached.toPrayerDay()
        }
        
        // Fetch from API
        return try {
            val res = api.timingsByCity(
                date = dateDdMmYyyy,
                city = city,
                country = country,
                method = method.apiId
            )
            val prayerDay = res.toPrayerDay()
            
            // Cache the result
            prayerDao.insertPrayerDay(PrayerDayEntity.fromPrayerDay(dateKey, prayerDay))
            Log.d(TAG, "Cached new data for $dateKey")
            
            prayerDay
        } catch (e: Exception) {
            Log.e(TAG, "API error, trying cache: ${e.message}")
            // If API fails, return cached data if available
            cached?.toPrayerDay() ?: throw e
        }
    }

    /**
     * Fetch and cache prayer times for the next 7 days
     */
    suspend fun fetchAndCacheWeek(
        lat: Double,
        lon: Double,
        method: CalculationMethod
    ): List<PrayerDay> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val results = mutableListOf<PrayerDay>()
        
        for (i in 0 until CACHE_DAYS) {
            val date = today.plusDays(i.toLong())
            val dateStr = date.format(formatter)
            
            try {
                val prayerDay = getPrayerDayByCoordinates(dateStr, lat, lon, method)
                results.add(prayerDay)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch day $i: ${e.message}")
            }
        }
        
        // Clean up old data
        val oldDate = today.minusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        prayerDao.deleteOldData(oldDate)
        
        results
    }

    /**
     * Fetch and cache prayer times for the next 7 days using Calendar API (single request)
     */
    suspend fun fetchAndCacheWeekByCity(
        city: String,
        country: String,
        method: CalculationMethod
    ): List<PrayerDay> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val results = mutableListOf<PrayerDay>()
        
        try {
            // Fetch current month calendar (single API call)
            val currentMonthCalendar = api.getCalendarByCity(
                year = today.year,
                month = today.monthValue,
                city = city,
                country = country,
                method = method.apiId
            )
            
            // If we're near end of month, also fetch next month
            val nextMonthCalendar = if (today.dayOfMonth > 24) {
                val nextMonth = today.plusMonths(1)
                api.getCalendarByCity(
                    year = nextMonth.year,
                    month = nextMonth.monthValue,
                    city = city,
                    country = country,
                    method = method.apiId
                )
            } else null
            
            // Combine and filter days for the week
            val allDays = currentMonthCalendar.data + (nextMonthCalendar?.data ?: emptyList())
            
            for (i in 0 until CACHE_DAYS) {
                val targetDate = today.plusDays(i.toLong())
                val targetDateStr = targetDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                
                // Find matching day in calendar response
                val dayData = allDays.find { calendarDay ->
                    calendarDay.date.gregorian?.date == targetDateStr ||
                    calendarDay.date.readable.contains("${targetDate.dayOfMonth} ")
                }
                
                if (dayData != null) {
                    val prayerDay = calendarDayToPrayerDay(dayData)
                    results.add(prayerDay)
                    
                    // Cache the result
                    val dateKey = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    prayerDao.insertPrayerDay(PrayerDayEntity.fromPrayerDay(dateKey, prayerDay))
                    Log.d(TAG, "Cached calendar data for $dateKey")
                }
            }
            
            Log.d(TAG, "Successfully fetched and cached ${results.size} days from calendar API")
            
        } catch (e: Exception) {
            Log.e(TAG, "Calendar API error, falling back to individual requests: ${e.message}")
            // Fallback to individual requests if calendar fails
            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            for (i in 0 until CACHE_DAYS) {
                val date = today.plusDays(i.toLong())
                val dateStr = date.format(formatter)
                try {
                    val prayerDay = getPrayerDayByCity(dateStr, city, country, method)
                    results.add(prayerDay)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch day $i: ${e.message}")
                }
            }
        }
        
        // Clean up old data
        val oldDate = today.minusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        prayerDao.deleteOldData(oldDate)
        
        results
    }


    /**
     * Get cached prayer times for a date range
     */
    suspend fun getCachedPrayerDays(startDate: LocalDate, endDate: LocalDate): List<PrayerDay> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val startKey = startDate.format(formatter)
        val endKey = endDate.format(formatter)
        
        return prayerDao.getPrayerDaysInRange(startKey, endKey).map { it.toPrayerDay() }
    }

    /**
     * Check if we have cached data for today
     */
    suspend fun hasCachedDataForToday(): Boolean {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return prayerDao.hasDataForDate(today) > 0
    }

    private fun com.AbuAlaa.data.remote.AladhanResponse.toPrayerDay(): PrayerDay {
        val t = data.timings
        val hijri = data.date.hijri
        
        // Convert to Arabic numerals
        val arabicDay = convertToArabicNumerals(hijri.day)
        val arabicYear = convertToArabicNumerals(hijri.year)
        val monthName = hijri.month.ar
        
        // Format: ٤ شعبان ١٤٤٧هـ
        val formattedHijriDate = "$arabicDay $monthName $arabicYear" + "هـ"
        
        return PrayerDay(
            imsak = TimeUtils.normalizeTime(t.imsak),
            fajr = TimeUtils.normalizeTime(t.fajr),
            sunrise = TimeUtils.normalizeTime(t.sunrise),
            dhuhr = TimeUtils.normalizeTime(t.dhuhr),
            asr = TimeUtils.normalizeTime(t.asr),
            maghrib = TimeUtils.normalizeTime(t.maghrib),
            isha = TimeUtils.normalizeTime(t.isha),
            timezone = data.meta.timezone,
            gregorianDate = data.date.readable,
            hijriDate = formattedHijriDate,
            hijriMonthNumber = data.date.hijri.month.number
        )
    }

    /**
     * Convert CalendarDayData to PrayerDay
     */
    private fun calendarDayToPrayerDay(dayData: com.AbuAlaa.data.remote.CalendarDayData): PrayerDay {
        val t = dayData.timings
        val hijri = dayData.date.hijri
        
        // Convert to Arabic numerals
        val arabicDay = convertToArabicNumerals(hijri.day)
        val arabicYear = convertToArabicNumerals(hijri.year)
        val monthName = hijri.month.ar
        
        // Format: ٤ شعبان ١٤٤٧هـ
        val formattedHijriDate = "$arabicDay $monthName $arabicYear" + "هـ"
        
        return PrayerDay(
            imsak = TimeUtils.normalizeTime(t.imsak),
            fajr = TimeUtils.normalizeTime(t.fajr),
            sunrise = TimeUtils.normalizeTime(t.sunrise),
            dhuhr = TimeUtils.normalizeTime(t.dhuhr),
            asr = TimeUtils.normalizeTime(t.asr),
            maghrib = TimeUtils.normalizeTime(t.maghrib),
            isha = TimeUtils.normalizeTime(t.isha),
            timezone = dayData.meta.timezone,
            gregorianDate = dayData.date.readable,
            hijriDate = formattedHijriDate,
            hijriMonthNumber = hijri.month.number
        )
    }
    
    private fun convertToArabicNumerals(number: String): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        return number.map { char ->
            if (char.isDigit()) arabicDigits[char.digitToInt()] else char
        }.joinToString("")
    }
    
    /**
     * Convert dd-MM-yyyy to yyyy-MM-dd for database key
     */
    private fun convertToDateKey(dateDdMmYyyy: String): String {
        return try {
            val parts = dateDdMmYyyy.split("-")
            "${parts[2]}-${parts[1]}-${parts[0]}"
        } catch (e: Exception) {
            dateDdMmYyyy
        }
    }
    
    /**
     * Check if cached data is expired (older than 6 hours)
     */
    private fun isCacheExpired(entity: PrayerDayEntity): Boolean {
        val sixHoursAgo = System.currentTimeMillis() - (6 * 60 * 60 * 1000)
        return entity.cachedAt < sixHoursAgo
    }
}
