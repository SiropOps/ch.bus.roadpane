package ch.bus.roadpanel.feature.energy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.bus.roadpanel.feature.energy.domain.VictronRepository
import ch.bus.roadpanel.feature.energy.domain.WaitingForMqttDataException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EnergyViewModel(private val repository: VictronRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(EnergyUiState(isLoading = true))
    val uiState: StateFlow<EnergyUiState> = _uiState.asStateFlow()

    init {
        startAutoRefresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loadEnergy(showLoading = true)
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                loadEnergy(showLoading = _uiState.value.metrics == null && _uiState.value.errorMessage == null)
                delay(5_000)
            }
        }
    }

    private suspend fun loadEnergy(showLoading: Boolean) {
        if (showLoading) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                waitingForMqttData = false,
            )
        }

        val health = runCatching { repository.getHealth() }
            .getOrElse { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Unable to reach Victron health endpoint",
                    waitingForMqttData = false,
                )
                return
            }

        runCatching { repository.getMetrics() }
            .onSuccess { metrics ->
                _uiState.value = EnergyUiState(
                    health = health,
                    metrics = metrics,
                    lastUpdated = metrics.timestamp,
                )
            }
            .onFailure { exception ->
                if (exception is WaitingForMqttDataException) {
                    _uiState.value = EnergyUiState(
                        health = health,
                        waitingForMqttData = true,
                        lastUpdated = health.lastMessageTimestamp,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        health = health,
                        errorMessage = exception.message ?: "Unable to read Victron metrics",
                        waitingForMqttData = false,
                    )
                }
            }
    }

    companion object {
        fun factory(repository: VictronRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T = EnergyViewModel(repository) as T
            }
    }
}
