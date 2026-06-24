package ch.bus.roadpanel.feature.sensors.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.bus.roadpanel.feature.sensors.data.SensorHistoryResponseDto
import ch.bus.roadpanel.feature.sensors.domain.SensorsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SensorHistoryUiState(
    val isLoading: Boolean = true,
    val history: SensorHistoryResponseDto? = null,
    val errorMessage: String? = null,
)

class SensorHistoryViewModel(
    private val sensorId: String,
    private val repository: SensorsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SensorHistoryUiState())
    val uiState: StateFlow<SensorHistoryUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.getHistory(sensorId) }
                .onSuccess { _uiState.value = SensorHistoryUiState(isLoading = false, history = it) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = it.message ?: "Impossible de charger l’historique",
                    )
                }
        }
    }

    companion object {
        fun factory(sensorId: String, repository: SensorsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SensorHistoryViewModel(sensorId, repository) as T
            }
    }
}
