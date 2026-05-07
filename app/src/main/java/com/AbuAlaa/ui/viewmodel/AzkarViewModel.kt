
package com.AbuAlaa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.AbuAlaa.data.repo.AzkarRepository
import com.AbuAlaa.data.repo.AzkarType
import com.AbuAlaa.data.repo.ZikrItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AzkarUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val morning: List<ZikrItem> = emptyList(),
    val evening: List<ZikrItem> = emptyList()
)

@HiltViewModel
class AzkarViewModel @Inject constructor(
    private val repo: AzkarRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AzkarUiState())
    val state: StateFlow<AzkarUiState> = _state

    init {
        refresh()
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val morning = runCatching { repo.getAzkar(AzkarType.MORNING, force) }.getOrNull()
            val evening = runCatching { repo.getAzkar(AzkarType.EVENING, force) }.getOrNull()
            if (morning == null || evening == null) {
                _state.value = _state.value.copy(isLoading = false, error = "Failed to load azkar")
            } else {
                _state.value = _state.value.copy(isLoading = false, morning = morning, evening = evening, error = null)
            }
        }
    }
}
