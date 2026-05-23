package ch.bus.roadpanel.feature.vpn.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class VanConnectivityRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(1_500, TimeUnit.MILLISECONDS)
        .readTimeout(1_500, TimeUnit.MILLISECONDS)
        .callTimeout(1_500, TimeUnit.MILLISECONDS)
        .build(),
) {
    suspend fun checkStatus(): VanConnectivityStatus = withContext(Dispatchers.IO) {
        coroutineScope {
            val gpsReachable = async { endpointResponds(GPS_ENDPOINT) }
            val victronReachable = async { endpointResponds(VICTRON_HEALTH_ENDPOINT) }

            if (gpsReachable.await() || victronReachable.await()) {
                VanConnectivityStatus.ONLINE
            } else {
                VanConnectivityStatus.OFFLINE
            }
        }
    }

    private fun endpointResponds(url: String): Boolean =
        runCatching {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        }.getOrDefault(false)

    private companion object {
        const val GPS_ENDPOINT = "http://192.168.255.100:8011/api/gps"
        const val VICTRON_HEALTH_ENDPOINT = "http://192.168.255.100:8013/api/health"
    }
}
