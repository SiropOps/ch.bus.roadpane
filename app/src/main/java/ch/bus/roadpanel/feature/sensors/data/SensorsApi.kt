package ch.bus.roadpanel.feature.sensors.data

import retrofit2.http.GET

interface SensorsApi {
    @GET("api/sensors")
    suspend fun getSensors(): SensorsResponseDto
}
