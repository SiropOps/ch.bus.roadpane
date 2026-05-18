package ch.bus.roadpanel.feature.gps.data

data class GpsDto(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed: Double,
    val track: Double,
    val time: String,
)
