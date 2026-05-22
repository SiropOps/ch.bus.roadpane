package ch.bus.roadpanel.feature.control.domain

import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface ArduinoBluetoothController {
    val events: Flow<ArduinoBluetoothEvent>

    fun isBluetoothAvailable(): Boolean
    fun isBluetoothEnabled(): Boolean
    fun hasRequiredPermissions(): Boolean

    suspend fun connect()
    suspend fun disconnect()
    suspend fun setWifiEnabled(enabled: Boolean)
    fun close()
}

data class ArduinoBluetoothConfig(
    val deviceName: String = "SmartArduino",
    val fallbackAddress: String = "FC:A8:9A:00:0D:9E",
    val serviceUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"),
    val appendNewlineToCommand: Boolean = true,
    val connectTimeoutMillis: Long = 10_000,
)

sealed interface ArduinoBluetoothEvent {
    data object Connected : ArduinoBluetoothEvent
    data object Disconnected : ArduinoBluetoothEvent
    data class WifiState(val enabled: Boolean, val rawResponse: String) : ArduinoBluetoothEvent
    data class Response(val rawResponse: String) : ArduinoBluetoothEvent
    data class Error(val message: String) : ArduinoBluetoothEvent
}
