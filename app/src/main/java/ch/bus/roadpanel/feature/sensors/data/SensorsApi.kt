package ch.bus.roadpanel.feature.sensors.data

import retrofit2.http.GET
import retrofit2.http.Query

interface SensorsApi {
    @GET("api/sensors")
    suspend fun getSensors(): SensorsResponseDto

    @GET("api/history")
    suspend fun getHistory(@Query("sensor_id") sensorId: String): SensorHistoryResponseDto
}
