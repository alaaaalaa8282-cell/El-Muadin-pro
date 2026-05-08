package com.AbuAlaa.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "muadhin_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val LOCATION_MODE = stringPreferencesKey("location_mode")
        val MANUAL_CITY = stringPreferencesKey("manual_city")
        val MANUAL_COUNTRY = stringPreferencesKey("manual_country")
        val CALC_METHOD = stringPreferencesKey("calc_method")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val ADHAN_SOUND = stringPreferencesKey("adhan_sound")
        val PLAY_FULL_ADHAN = booleanPreferencesKey("play_full_adhan")
        val SILENT_FAJR = booleanPreferencesKey("silent_fajr")
        val LAST_DATE = stringPreferencesKey("last_prayer_date")
        val IMSAK = stringPreferencesKey("last_imsak")
        val FAJR = stringPreferencesKey("last_fajr")
        val SUNRISE = stringPreferencesKey("last_sunrise")
        val DHUHR = stringPreferencesKey("last_dhuhr")
        val ASR = stringPreferencesKey("last_asr")
        val MAGHRIB = stringPreferencesKey("last_maghrib")
        val ISHA = stringPreferencesKey("last_isha")
        val TIMEZONE = stringPreferencesKey("last_timezone")
        val SALAH_ENABLED = booleanPreferencesKey("salah_enabled")
        val SALAH_INTERVAL = intPreferencesKey("salah_interval")
        val SALAH_SOUND = stringPreferencesKey("salah_sound")
       val CACHED_LAT = stringPreferencesKey("cached_lat")
       val CACHED_LNG = stringPreferencesKey("cached_lng")
       val CACHED_CITY = stringPreferencesKey("cached_city")
       val LAST_PRAYER_UPDATE = stringPreferencesKey("last_prayer_update")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            locationMode = prefs[Keys.LOCATION_MODE]
                ?.let { runCatching { LocationMode.valueOf(it) }.getOrNull() }
                ?: LocationMode.AUTO,
            manualCity = prefs[Keys.MANUAL_CITY] ?: "",
            manualCountry = prefs[Keys.MANUAL_COUNTRY] ?: "",
            calculationMethod = prefs[Keys.CALC_METHOD]
                ?.let { runCatching { CalculationMethod.valueOf(it) }.getOrNull() }
                ?: CalculationMethod.UMM_AL_QURA,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            adhanSound = prefs[Keys.ADHAN_SOUND]
                ?.let { runCatching { AdhanSound.valueOf(it) }.getOrNull() }
                ?: AdhanSound.MAKKAH,
            silentFajr = prefs[Keys.SILENT_FAJR] ?: false,
            playFullAdhan = prefs[Keys.PLAY_FULL_ADHAN] ?: false,
            salahEnabled = prefs[Keys.SALAH_ENABLED] ?: false,
            salahSound = prefs[Keys.SALAH_SOUND]
                ?.let { runCatching { SalahSound.valueOf(it) }.getOrNull() }
                ?: SalahSound.NOZAKER,
            salahInterval = prefs[Keys.SALAH_INTERVAL] ?: 30,
        )
    }

    // ← ده اللي بيناديه HomeViewModel و PrayerAlarmScheduler
    val storedPrayerDayFlow: Flow<Pair<String, PrayerDay>?> =
        context.dataStore.data.map { prefs ->
            val date = prefs[Keys.LAST_DATE] ?: return@map null
            val day = PrayerDay(
                imsak = prefs[Keys.IMSAK] ?: return@map null,
                fajr = prefs[Keys.FAJR] ?: return@map null,
                sunrise = prefs[Keys.SUNRISE] ?: return@map null,
                dhuhr = prefs[Keys.DHUHR] ?: return@map null,
                asr = prefs[Keys.ASR] ?: return@map null,
                maghrib = prefs[Keys.MAGHRIB] ?: return@map null,
                isha = prefs[Keys.ISHA] ?: return@map null,
                timezone = prefs[Keys.TIMEZONE] ?: return@map null,
                gregorianDate = date,
                hijriDate = "",
                hijriMonthNumber = 0
            )
            Pair(date, day)
        }


    suspend fun getSettings(): UserSettings = settingsFlow.first()

    suspend fun updateSettings(update: (UserSettings) -> UserSettings) {
        val current = settingsFlow.first()
        val next = update(current)
        context.dataStore.edit { prefs ->
            prefs[Keys.LOCATION_MODE] = next.locationMode.name
            prefs[Keys.MANUAL_CITY] = next.manualCity
            prefs[Keys.MANUAL_COUNTRY] = next.manualCountry
            prefs[Keys.CALC_METHOD] = next.calculationMethod.name
            prefs[Keys.NOTIFICATIONS] = next.notificationsEnabled
            prefs[Keys.ADHAN_SOUND] = next.adhanSound.name
            prefs[Keys.PLAY_FULL_ADHAN] = next.playFullAdhan
            prefs[Keys.SILENT_FAJR] = next.silentFajr
            prefs[Keys.SALAH_ENABLED] = next.salahEnabled
            prefs[Keys.SALAH_SOUND] = next.salahSound.name
            prefs[Keys.SALAH_INTERVAL] = next.salahInterval
        }
    }

    suspend fun saveLocationCache(lat: Double, lng: Double, city: String) =
        context.dataStore.edit {
            it[Keys.CACHED_LAT] = lat.toString()
            it[Keys.CACHED_LNG] = lng.toString()
            it[Keys.CACHED_CITY] = city
        }

    suspend fun getLocationCache(): Triple<Double, Double, String>? {
        val prefs = context.dataStore.data.first()
        val lat = prefs[Keys.CACHED_LAT]?.toDoubleOrNull() ?: return null
        val lng = prefs[Keys.CACHED_LNG]?.toDoubleOrNull() ?: return null
        val city = prefs[Keys.CACHED_CITY] ?: return null
        return Triple(lat, lng, city)
    }

    suspend fun clearLocationCache() =
        context.dataStore.edit {
            it.remove(Keys.CACHED_LAT)
            it.remove(Keys.CACHED_LNG)
            it.remove(Keys.CACHED_CITY)
        }

    suspend fun setLocationMode(mode: LocationMode) =
        context.dataStore.edit { it[Keys.LOCATION_MODE] = mode.name }

    suspend fun setManualLocation(city: String, country: String) =
        context.dataStore.edit {
            it[Keys.MANUAL_CITY] = city
            it[Keys.MANUAL_COUNTRY] = country
        }

    suspend fun setCalculationMethod(method: CalculationMethod) =
        context.dataStore.edit { it[Keys.CALC_METHOD] = method.name }

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }



    suspend fun setAdhanSound(sound: AdhanSound) =
        context.dataStore.edit { it[Keys.ADHAN_SOUND] = sound.name }

    suspend fun setSilentFajr(silent: Boolean) =
        context.dataStore.edit { it[Keys.SILENT_FAJR] = silent }

    suspend fun setPlayFullAdhan(playFull: Boolean) =
        context.dataStore.edit { it[Keys.PLAY_FULL_ADHAN] = playFull }

    suspend fun setSalahEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.SALAH_ENABLED] = enabled }

    suspend fun setSalahSound(sound: SalahSound) =
        context.dataStore.edit { it[Keys.SALAH_SOUND] = sound.name }

    suspend fun setSalahInterval(minutes: Int) =
        context.dataStore.edit { it[Keys.SALAH_INTERVAL] = minutes }

    suspend fun savePrayerDayForReschedule(dateIso: String, day: PrayerDay) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_DATE] = dateIso
            prefs[Keys.IMSAK] = day.imsak
            prefs[Keys.FAJR] = day.fajr
            prefs[Keys.SUNRISE] = day.sunrise
            prefs[Keys.DHUHR] = day.dhuhr
            prefs[Keys.ASR] = day.asr
            prefs[Keys.MAGHRIB] = day.maghrib
            prefs[Keys.ISHA] = day.isha
            prefs[Keys.TIMEZONE] = day.timezone
        }
    }

    suspend fun getLastPrayerTimes(): PrayerDay? =
        storedPrayerDayFlow.first()?.second

    suspend fun savePrayerUpdateDate(dateIso: String) =
        context.dataStore.edit { it[Keys.LAST_PRAYER_UPDATE] = dateIso }

    suspend fun getLastPrayerUpdateDate(): String? {
        val prefs = context.dataStore.data.first()
        return prefs[Keys.LAST_PRAYER_UPDATE]
    }

}
