package com.AbuAlaa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.AbuAlaa.alarm.PrayerAlarmScheduler
import com.AbuAlaa.data.LocationMode
import com.AbuAlaa.data.PrayerDay
import com.AbuAlaa.data.SettingsRepository
import com.AbuAlaa.data.UserSettings
import com.AbuAlaa.data.repo.LocationRepository
import com.AbuAlaa.data.repo.PrayerRepository
import com.AbuAlaa.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val settings: UserSettings = UserSettings(),
    val day: PrayerDay? = null,
    val selectedDateDay: PrayerDay? = null,
    val nextPrayerName: String? = null,
    val nextPrayerTime: String? = null,
    val countdown: String? = null,
    val isOffline: Boolean = false,
    val lastUpdated: String? = null,
    val cityName: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val settingsRepo: SettingsRepository,
    private val prayerRepo: PrayerRepository,
    private val locationRepo: LocationRepository,
    private val scheduler: PrayerAlarmScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    private val zoneId = ZoneId.systemDefault()

    init {
        viewModelScope.launch {
            loadCachedPrayerTimes()
            settingsRepo.settingsFlow.collect { s ->
                _state.value = _state.value.copy(settings = s)
                refresh()
            }
        }
        startTicker()
    }

    private suspend fun loadCachedPrayerTimes() {
        if (prayerRepo.hasCachedDataForToday()) {
            val date = TimeUtils.todayDdMmYyyy(zoneId)
            val s = settingsRepo.settingsFlow.first()
            try {
                val cachedLoc = settingsRepo.getLocationCache()
                // *** التعديل هنا: val day = ***
                val day = if (cachedLoc != null) {
                    // حفظ اسم المدينة من الـ cache مباشرة بدون loading
                    _state.value = _state.value.copy(cityName = cachedLoc.third)
                    prayerRepo.getPrayerDayByCoordinates(date, cachedLoc.first, cachedLoc.second, s.calculationMethod)
                } else {
                    val loc = locationRepo.getLastKnownLocation()
                    if (loc != null) {
                        val geocoder = android.location.Geocoder(context, java.util.Locale("ar"))
                        val address = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        val city = address?.firstOrNull()?.locality
                            ?: address?.firstOrNull()?.subAdminArea
                            ?: address?.firstOrNull()?.adminArea ?: ""
                        settingsRepo.saveLocationCache(loc.latitude, loc.longitude, city)
                        _state.value = _state.value.copy(cityName = city)
                        prayerRepo.getPrayerDayByCoordinates(date, loc.latitude, loc.longitude, s.calculationMethod)
                    } else {
                        prayerRepo.getPrayerDayByCity(date, s.manualCity, s.manualCountry, s.calculationMethod)
                    }
                }

                _state.value = _state.value.copy(
                    day = day,
                    isLoading = false,
                    isOffline = false,
                    lastUpdated = "محفوظ مسبقاً",
                    error = null
                )
                computeNext(day)
            } catch (e: Exception) {
                val cached = settingsRepo.storedPrayerDayFlow.first()
                if (cached != null) {
                    _state.value = _state.value.copy(
                        day = cached.second,
                        isLoading = false,
                        isOffline = true,
                        lastUpdated = "محفوظ مسبقاً",
                        error = null
                    )
                    computeNext(cached.second)
                }
            }
        } else {
            val cached = settingsRepo.storedPrayerDayFlow.first()
            val todayIso = TimeUtils.todayIso(zoneId)
            if (cached != null && cached.first == todayIso) {
                _state.value = _state.value.copy(
                    day = cached.second,
                    isLoading = false,
                    isOffline = true,
                    lastUpdated = "محفوظ مسبقاً",
                    error = null
                )
                computeNext(cached.second)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = _state.value.day == null, error = null)
            val s = settingsRepo.settingsFlow.first()
            _state.value = _state.value.copy(settings = s)

            val date = TimeUtils.todayDdMmYyyy(zoneId)
            val day = runCatching {
                if (s.locationMode == LocationMode.AUTO) {
                    val cachedLoc = settingsRepo.getLocationCache()
                    if (cachedLoc != null) {
                        // يستخدم الـ cache مباشرة بدون GPS - يحدث cityName من المحفوظ
                        _state.value = _state.value.copy(cityName = cachedLoc.third)
                        prayerRepo.getPrayerDayByCoordinates(date, cachedLoc.first, cachedLoc.second, s.calculationMethod)
                    } else {
                        val loc = locationRepo.getLastKnownLocation()
                        if (loc != null) {
                            val geocoder = android.location.Geocoder(context, java.util.Locale("ar"))
                            val address = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                            val city = address?.firstOrNull()?.locality
                                ?: address?.firstOrNull()?.subAdminArea
                                ?: address?.firstOrNull()?.adminArea ?: ""
                            settingsRepo.saveLocationCache(loc.latitude, loc.longitude, city)
                            _state.value = _state.value.copy(cityName = city)
                            prayerRepo.getPrayerDayByCoordinates(date, loc.latitude, loc.longitude, s.calculationMethod)
                        } else {
                            prayerRepo.getPrayerDayByCity(date, s.manualCity, s.manualCountry, s.calculationMethod)
                        }
                    }
                } else {
                    prayerRepo.getPrayerDayByCity(date, s.manualCity, s.manualCountry, s.calculationMethod)
                }
            }.getOrElse { e ->
                val cached = settingsRepo.storedPrayerDayFlow.first()
                if (cached != null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        day = cached.second,
                        isOffline = true,
                        lastUpdated = "آخر تحديث: ${cached.first}",
                        error = null
                    )
                    computeNext(cached.second)
                    if (s.notificationsEnabled && scheduler.canScheduleExactAlarms()) {
                        scheduler.cancelAll()
                        scheduler.scheduleForToday(cached.second, s.adhanSound.name, zoneId)
                    }
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "لا يوجد اتصال بالإنترنت ولا توجد بيانات محفوظة",
                        isOffline = true
                    )
                }
                return@launch
            }

            _state.value = _state.value.copy(
                isLoading = false,
                day = day,
                error = null,
                isOffline = false,
                lastUpdated = "تم التحديث الآن"
            )

            runCatching {
                settingsRepo.savePrayerDayForReschedule(TimeUtils.todayIso(zoneId), day)
            }

            if (s.notificationsEnabled && scheduler.canScheduleExactAlarms()) {
                scheduler.cancelAll()
                scheduler.scheduleForToday(day, s.adhanSound.name, zoneId)
            }

            computeNext(day)

            launch {
                runCatching {
                    if (s.locationMode == LocationMode.AUTO) {
                        // نستخدم الـ cache مباشرة بدون GPS call جديدة
                        val cachedLoc = settingsRepo.getLocationCache()
                        if (cachedLoc != null) {
                            prayerRepo.fetchAndCacheWeek(cachedLoc.first, cachedLoc.second, s.calculationMethod)
                        } else {
                            val loc = locationRepo.getFreshLocation()
                            if (loc != null) {
                                prayerRepo.fetchAndCacheWeek(loc.latitude, loc.longitude, s.calculationMethod)
                            } else {
                                prayerRepo.fetchAndCacheWeekByCity(s.manualCity, s.manualCountry, s.calculationMethod)
                            }
                        }
                    } else {
                        prayerRepo.fetchAndCacheWeekByCity(s.manualCity, s.manualCountry, s.calculationMethod)
                    }
                }
            }
        }
    }

    fun fetchPrayerTimesForDate(date: LocalDate) {
        viewModelScope.launch {
            val s = settingsRepo.settingsFlow.first()
            val dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            runCatching {
                val day = if (s.locationMode == LocationMode.AUTO) {
                    val loc = locationRepo.getLastKnownLocation()
                    if (loc != null) {
                        prayerRepo.getPrayerDayByCoordinates(dateStr, loc.latitude, loc.longitude, s.calculationMethod)
                    } else {
                        prayerRepo.getPrayerDayByCity(dateStr, s.manualCity, s.manualCountry, s.calculationMethod)
                    }
                } else {
                    prayerRepo.getPrayerDayByCity(dateStr, s.manualCity, s.manualCountry, s.calculationMethod)
                }
                _state.value = _state.value.copy(selectedDateDay = day)
            }.onFailure {
                _state.value = _state.value.copy(selectedDateDay = _state.value.day)
            }
        }
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                val d = _state.value.day
                if (d != null) computeNext(d)
                delay(1000L)
            }
        }
    }

    private fun computeNext(day: PrayerDay) {
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zoneId)

        val schedule = listOf(
            "الفجر" to day.fajr,
            "الظهر" to day.dhuhr,
            "العصر" to day.asr,
            "المغرب" to day.maghrib,
            "العشاء" to day.isha
        )

        var nextName: String? = null
        var nextTime: String? = null
        var nextMillis: Long? = null

        for ((name, time) in schedule) {
            val ms = TimeUtils.toMillis(today, time, zoneId)
            if (ms > now) {
                nextName = name
                nextTime = time
                nextMillis = ms
                break
            }
        }

        if (nextMillis == null) {
            nextName = "الفجر"
            nextTime = day.fajr
            nextMillis = TimeUtils.toMillis(today.plusDays(1), day.fajr, zoneId)
        }

        _state.value = _state.value.copy(
            nextPrayerName = nextName,
            nextPrayerTime = nextTime,
            countdown = TimeUtils.formatDuration(TimeUtils.millisUntil(nextMillis))
        )
    }
}
