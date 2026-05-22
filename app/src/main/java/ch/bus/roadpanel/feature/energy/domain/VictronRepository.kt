package ch.bus.roadpanel.feature.energy.domain

import ch.bus.roadpanel.feature.energy.data.VictronApi
import ch.bus.roadpanel.feature.energy.data.VictronHealthDto
import ch.bus.roadpanel.feature.energy.data.VictronMetricsDto
import retrofit2.HttpException

class VictronRepository(private val api: VictronApi) {
    suspend fun getHealth(): VictronHealthDto = api.getHealth()

    suspend fun getMetrics(): VictronMetricsDto {
        return try {
            api.getMetrics()
        } catch (exception: HttpException) {
            if (exception.code() == 503) {
                throw WaitingForMqttDataException()
            }
            throw exception
        }
    }
}

class WaitingForMqttDataException : Exception("En attente des données MPPT")
