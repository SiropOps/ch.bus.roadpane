package ch.bus.roadpanel.feature.gps.data

import retrofit2.http.GET

interface GpsApi {
    @GET("api/gps")
    suspend fun getGps(): GpsDto

    @GET("api/gps/status")
    suspend fun getGpsStatus(): List<GpsStatusDto>
}
