package ch.bus.roadpanel.feature.sensors.ui

import ch.bus.roadpanel.feature.sensors.data.SensorsResponseDto

data class SensorsUiState(
    val isLoading: Boolean = false,
    val response: SensorsResponseDto? = null,
    val errorMessage: String? = null,
)
