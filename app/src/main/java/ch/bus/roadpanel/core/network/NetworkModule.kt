package ch.bus.roadpanel.core.network

import ch.bus.roadpanel.feature.gps.data.GpsApi
import ch.bus.roadpanel.feature.energy.data.VictronApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    private const val GPS_BASE_URL = "http://192.168.255.100:8011/"
    private const val VICTRON_BASE_URL = "http://192.168.255.100:8013/"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    val gpsApi: GpsApi by lazy {
        retrofit(GPS_BASE_URL)
            .create(GpsApi::class.java)
    }

    val victronApi: VictronApi by lazy {
        retrofit(VICTRON_BASE_URL)
            .create(VictronApi::class.java)
    }

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}
