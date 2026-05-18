package ch.bus.roadpanel.feature.gps.domain

import ch.bus.roadpanel.feature.gps.data.GpsApi
import ch.bus.roadpanel.feature.gps.ui.GpsReading

class GpsRepository(private val api: GpsApi) {
    suspend fun fetchGps(): GpsReading {
        val dto = api.getGps()
        return GpsReading(
            mapLatitude = dto.longitude,
            mapLongitude = dto.latitude,
            altitude = dto.altitude,
            speed = dto.speed,
            track = dto.track,
            time = dto.time,
        )
    }
}
