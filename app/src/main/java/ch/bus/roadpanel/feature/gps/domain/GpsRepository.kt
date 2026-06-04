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
            speed = dto.speed * METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR,
            track = dto.track,
            time = dto.time,
        )
    }

    private companion object {
        const val METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR = 3.6
    }
}
