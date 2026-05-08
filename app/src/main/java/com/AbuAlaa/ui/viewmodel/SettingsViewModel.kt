package com.AbuAlaa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.AbuAlaa.data.AdhanSound
import com.AbuAlaa.data.SalahSound
import com.AbuAlaa.data.CalculationMethod
import com.AbuAlaa.data.LocationMode
import com.AbuAlaa.data.SettingsRepository
import com.AbuAlaa.data.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(UserSettings())
    val ui: StateFlow<UserSettings> = _ui

    init {
        viewModelScope.launch {
            repo.settingsFlow.collect { _ui.value = it }
        }
    }

    fun setLocationMode(mode: LocationMode) = viewModelScope.launch {
        repo.setLocationMode(mode)
        // لو رجع لـ AUTO امسح الـ cache اليدوي عشان يجيب GPS جديد
        if (mode == LocationMode.AUTO) repo.clearLocationCache()
    }
    fun setManual(city: String, country: String) = viewModelScope.launch {
        repo.setManualLocation(city, country)
        repo.clearLocationCache()  // امسح الـ GPS cache عشان يستخدم المدينة اليدوية
    }
    fun setMethod(method: CalculationMethod) = viewModelScope.launch { repo.setCalculationMethod(method) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { repo.setNotificationsEnabled(enabled) }
    fun setAdhanSound(sound: AdhanSound) = viewModelScope.launch { repo.setAdhanSound(sound) }
    fun setSilentFajr(silent: Boolean) = viewModelScope.launch { repo.setSilentFajr(silent) }
    fun setPlayFullAdhan(playFull: Boolean) = viewModelScope.launch { repo.setPlayFullAdhan(playFull) }
   fun setSalahEnabled(enabled: Boolean) = viewModelScope.launch { repo.setSalahEnabled(enabled) }
    fun setSalahInterval(minutes: Int) = viewModelScope.launch { repo.setSalahInterval(minutes) }
   fun setSalahSound(sound: SalahSound) = viewModelScope.launch { repo.setSalahSound(sound) }
    suspend fun current(): UserSettings = repo.settingsFlow.first()
}
