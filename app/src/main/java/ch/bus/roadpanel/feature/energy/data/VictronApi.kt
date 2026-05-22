package ch.bus.roadpanel.feature.energy.data

import retrofit2.http.GET

interface VictronApi {
    @GET("api/health")
    suspend fun getHealth(): VictronHealthDto

    @GET("api/metrics")
    suspend fun getMetrics(): VictronMetricsDto
}
