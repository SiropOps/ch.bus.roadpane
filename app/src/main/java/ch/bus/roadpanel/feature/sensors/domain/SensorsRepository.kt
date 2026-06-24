package ch.bus.roadpanel.feature.sensors.domain

import ch.bus.roadpanel.feature.sensors.data.SensorsApi
import ch.bus.roadpanel.feature.sensors.data.SensorsResponseDto
import ch.bus.roadpanel.feature.sensors.data.SensorHistoryResponseDto

class SensorsRepository(private val api: SensorsApi) {
    suspend fun getSensors(): SensorsResponseDto = api.getSensors()
    suspend fun getHistory(sensorId: String): SensorHistoryResponseDto = api.getHistory(sensorId)
}
