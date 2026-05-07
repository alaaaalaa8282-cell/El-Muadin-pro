package com.AbuAlaa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.AbuAlaa.data.LocationMode
import com.AbuAlaa.data.PrayerDay
import com.AbuAlaa.data.SettingsRepository
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
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class RamadanUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val hijriDate: String? = null,
    val isRamadan: Boolean = false,
    val suhoorEndsAt: String? = null,
    val iftarAt: String? = null,
    val nextEventTitle: String? = null,
    val countdown: String? = null
)

@HiltViewModel
class RamadanViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val prayerRepo: PrayerRepository,
    private val locationRepo: LocationRepository
) : ViewModel() {

    private val zoneId = ZoneId.systemDefault()
    private val _state = MutableStateFlow(RamadanUiState())
    val state: StateFlow<RamadanUiState> = _state

    private var todayDay: PrayerDay? = null
    private var tomorrowDay: PrayerDay? = null

    init {
        refresh()
        startTicker()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val s = settingsRepo.settingsFlow.first()
            val today = LocalDate.now(zoneId)
            val fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val todayStr = today.format(fmt)
            val tomorrowStr = today.plusDays(1).format(fmt)

            suspend fun load(dateStr: String): PrayerDay {
                return if (s.locationMode == LocationMode.AUTO) {
                    val loc = locationRepo.getLastKnownLocation()
                    if (loc != null) {
                        prayerRepo.getPrayerDayByCoordinates(dateStr, loc.latitude, loc.longitude, s.calculationMethod)
                    } else {
                        prayerRepo.getPrayerDayByCity(dateStr, s.manualCity, s.manualCountry, s.calculationMethod)
                    }
                } else {
                    prayerRepo.getPrayerDayByCity(dateStr, s.manualCity, s.manualCountry, s.calculationMethod)
                }
            }

            val todayRes = runCatching { load(todayStr) }.getOrElse { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load Ramadan data"
                )
                return@launch
            }

            val tomorrowRes = runCatching { load(tomorrowStr) }.getOrNull()

            todayDay = todayRes
            tomorrowDay = tomorrowRes

            _state.value = _state.value.copy(
                isLoading = false,
                hijriDate = todayRes.hijriDate,
                isRamadan = todayRes.hijriMonthNumber == 9,
                suhoorEndsAt = todayRes.imsak,
                iftarAt = todayRes.maghrib,
                error = null
            )

            computeNext()
        }
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                computeNext()
                delay(1000)
            }
        }
    }

    private fun computeNext() {
        val today = LocalDate.now(zoneId)
        val now = System.currentTimeMillis()

        val d = todayDay ?: return

        val imsakMs = TimeUtils.toMillis(today, d.imsak, zoneId)
        val maghribMs = TimeUtils.toMillis(today, d.maghrib, zoneId)

        val (title, targetMs) = when {
            now < imsakMs -> "متبقي على الإمساك" to imsakMs
            now < maghribMs -> "متبقي على الإفطار" to maghribMs
            else -> {
                val nextImsak = (tomorrowDay?.imsak ?: d.imsak)
                "متبقي على إمساك الغد" to TimeUtils.toMillis(today.plusDays(1), nextImsak, zoneId)
            }
        }

        _state.value = _state.value.copy(
            nextEventTitle = title,
            countdown = TimeUtils.formatDuration(TimeUtils.millisUntil(targetMs))
        )
    }
}
