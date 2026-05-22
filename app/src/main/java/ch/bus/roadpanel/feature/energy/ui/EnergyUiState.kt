package ch.bus.roadpanel.feature.energy.ui

import ch.bus.roadpanel.feature.energy.data.VictronHealthDto
import ch.bus.roadpanel.feature.energy.data.VictronMetricsDto

data class EnergyUiState(
    val isLoading: Boolean = false,
    val health: VictronHealthDto? = null,
    val metrics: VictronMetricsDto? = null,
    val errorMessage: String? = null,
    val waitingForMqttData: Boolean = false,
    val lastUpdated: String? = null,
)
