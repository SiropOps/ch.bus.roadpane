package ch.bus.roadpanel.feature.gps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.bus.roadpanel.feature.gps.domain.GpsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GpsViewModel(private val repository: GpsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(GpsUiState(isLoading = true))
    val uiState: StateFlow<GpsUiState> = _uiState.asStateFlow()

    init {
        startAutoRefresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val gpsResult = runCatching { repository.fetchGps() }
            val statusResult = runCatching { repository.fetchGpsStatus() }
            val currentState = _uiState.value

            _uiState.value = currentState.copy(
                isLoading = false,
                data = gpsResult.getOrElse { currentState.data },
                sensorStatuses = statusResult.getOrElse { currentState.sensorStatuses },
                error = gpsResult.exceptionOrNull()?.message,
            )
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                refresh()
                delay(60_000)
            }
        }
    }

    companion object {
        fun factory(repository: GpsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T = GpsViewModel(repository) as T
            }
    }
}
