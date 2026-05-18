package ch.bus.roadpanel.feature.gps.ui

data class GpsReading(
    val mapLatitude: Double,
    val mapLongitude: Double,
    val altitude: Double,
    val speed: Double,
    val track: Double,
    val time: String,
)

data class GpsUiState(
    val isLoading: Boolean = false,
    val data: GpsReading? = null,
    val error: String? = null,
)
