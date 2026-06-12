package ch.bus.roadpanel.feature.control.domain

import ch.bus.roadpanel.BuildConfig
import ch.bus.roadpanel.core.network.NetworkModule
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.net.InetAddress

class VanShutdownRepository(
    private val host: String = NetworkModule.VAN_HOST,
    private val user: String = BuildConfig.VAN_SSH_USER,
    private val password: String = BuildConfig.VAN_SSH_PASSWORD,
) {
    suspend fun isVanReachable(): Boolean = withContext(Dispatchers.IO) {
        runCatching { pingHost(host) }.getOrDefault(false)
    }

    suspend fun shutdownNow() = withContext(Dispatchers.IO) {
        check(user.isNotBlank()) { "Utilisateur SSH manquant" }
        check(password.isNotBlank()) { "Mot de passe SSH manquant dans van-credentials.properties" }

        val session = JSch().getSession(user, host, SSH_PORT).apply {
            setPassword(password)
            setConfig("StrictHostKeyChecking", "no")
            timeout = SSH_CONNECT_TIMEOUT_MS
            connect(SSH_CONNECT_TIMEOUT_MS)
        }

        try {
            val channel = session.openChannel("exec") as ChannelExec
            val command = "shutdown now || sudo -S -p '' shutdown now"
            channel.setCommand(command)
            channel.setInputStream(ByteArrayInputStream("$password\n".toByteArray()))
            channel.connect(SSH_CONNECT_TIMEOUT_MS)

            val startedAt = System.currentTimeMillis()
            while (!channel.isClosed && System.currentTimeMillis() - startedAt < SSH_COMMAND_TIMEOUT_MS) {
                Thread.sleep(200)
            }
            channel.disconnect()
        } finally {
            session.disconnect()
        }
    }

    suspend fun waitUntilOffline(
        timeoutMillis: Long = SHUTDOWN_WAIT_TIMEOUT_MS,
        pingIntervalMillis: Long = PING_INTERVAL_MS,
        onPingAttempt: suspend (Int) -> Unit,
    ): Boolean {
        val startedAt = System.currentTimeMillis()
        var attempt = 0
        while (System.currentTimeMillis() - startedAt < timeoutMillis) {
            attempt += 1
            onPingAttempt(attempt)
            if (!isVanReachable()) {
                return true
            }
            delay(pingIntervalMillis)
        }
        return false
    }

    private fun pingHost(host: String): Boolean {
        val pingSucceeded = runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("/system/bin/ping", "-c", "1", "-W", "1", host))
            process.waitFor() == 0
        }.getOrDefault(false)
        return pingSucceeded || InetAddress.getByName(host).isReachable(PING_FALLBACK_TIMEOUT_MS)
    }

    private companion object {
        const val SSH_PORT = 22
        const val SSH_CONNECT_TIMEOUT_MS = 10_000
        const val SSH_COMMAND_TIMEOUT_MS = 10_000L
        const val SHUTDOWN_WAIT_TIMEOUT_MS = 30_000L
        const val PING_INTERVAL_MS = 2_000L
        const val PING_FALLBACK_TIMEOUT_MS = 1_000
    }
}
