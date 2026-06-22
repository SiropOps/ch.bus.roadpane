package ch.bus.roadpanel.feature.sensors.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.bus.roadpanel.feature.sensors.domain.SensorsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SensorsViewModel(private val repository: SensorsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SensorsUiState(isLoading = true))
    val uiState: StateFlow<SensorsUiState> = _uiState.asStateFlow()

    init { startAutoRefresh() }

    fun refresh() {
        viewModelScope.launch { loadSensors(showLoading = _uiState.value.response == null) }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                loadSensors(showLoading = _uiState.value.response == null)
                delay(5_000)
            }
        }
    }

    private suspend fun loadSensors(showLoading: Boolean) {
        if (showLoading) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        }
        runCatching { repository.getSensors() }
            .onSuccess { _uiState.value = SensorsUiState(response = it) }
            .onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Impossible de joindre les capteurs",
                )
            }
    }

    companion object {
        fun factory(repository: SensorsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SensorsViewModel(repository) as T
            }
    }
}
