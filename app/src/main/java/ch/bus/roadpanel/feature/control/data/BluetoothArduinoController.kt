package ch.bus.roadpanel.feature.control.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import ch.bus.roadpanel.feature.control.domain.ArduinoBluetoothConfig
import ch.bus.roadpanel.feature.control.domain.ArduinoBluetoothController
import ch.bus.roadpanel.feature.control.domain.ArduinoBluetoothEvent
import ch.bus.roadpanel.feature.control.domain.ArduinoResponseParser
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class BluetoothArduinoController(
    context: Context,
    private val config: ArduinoBluetoothConfig = ArduinoBluetoothConfig(),
) : ArduinoBluetoothController {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _events = MutableSharedFlow<ArduinoBluetoothEvent>(extraBufferCapacity = 32)

    private var socket: BluetoothSocket? = null
    private var readJob: Job? = null
    private var disconnectRequested = false

    override val events: Flow<ArduinoBluetoothEvent> = _events

    override fun isBluetoothAvailable(): Boolean = adapter != null

    override fun isBluetoothEnabled(): Boolean {
        if (!hasRequiredPermissions()) return false
        return runCatching { adapter?.isEnabled == true }.getOrDefault(false)
    }

    override fun hasRequiredPermissions(): Boolean = ControlPermissions.hasRequiredPermissions(appContext)

    @SuppressLint("MissingPermission")
    override suspend fun connect() {
        val bluetoothAdapter = adapter ?: throw BluetoothControlException("Le Bluetooth n'est pas disponible sur cet appareil")
        if (!hasRequiredPermissions()) throw BluetoothControlException("La permission Bluetooth est manquante")
        if (!bluetoothAdapter.isEnabled) throw BluetoothControlException("Le Bluetooth est désactivé")
        if (socket?.isConnected == true) {
            _events.emit(ArduinoBluetoothEvent.Connected)
            return
        }

        disconnectRequested = false
        withContext(Dispatchers.IO) {
            closeSocketOnly()
            val device = findDevice(bluetoothAdapter)
                ?: throw BluetoothControlException("Appareil SmartArduino introuvable")
            runCatching { cancelDiscoveryIfAllowed(bluetoothAdapter) }

            val newSocket = device.createRfcommSocketToServiceRecord(config.serviceUuid)
            try {
                withTimeout(config.connectTimeoutMillis) {
                    newSocket.connect()
                }
                socket = newSocket
                _events.emit(ArduinoBluetoothEvent.Connected)
                startReader(newSocket)
            } catch (exception: Exception) {
                runCatching { newSocket.close() }
                throw BluetoothControlException(
                    "Impossible de se connecter à ${config.deviceName} (${config.fallbackAddress})",
                    exception,
                )
            }
        }
    }

    override suspend fun disconnect() {
        disconnectRequested = true
        withContext(Dispatchers.IO) {
            closeSocketOnly()
            _events.emit(ArduinoBluetoothEvent.Disconnected)
        }
    }

    override suspend fun setWifiEnabled(enabled: Boolean) {
        val activeSocket = socket?.takeIf { it.isConnected }
            ?: throw BluetoothControlException("Le Bluetooth n'est pas connecté")
        val code = if (enabled) "1" else "0"
        val payload = if (config.appendNewlineToCommand) "$code\n" else code

        withContext(Dispatchers.IO) {
            activeSocket.outputStream.write(payload.toByteArray(StandardCharsets.US_ASCII))
            activeSocket.outputStream.flush()
            _events.emit(ArduinoBluetoothEvent.Response("Envoyé $code"))
        }
    }

    override fun close() {
        scope.cancel()
        runCatching { readJob?.cancel() }
        runCatching { socket?.close() }
        socket = null
    }

    @SuppressLint("MissingPermission")
    private fun findDevice(bluetoothAdapter: BluetoothAdapter) = runCatching {
        bluetoothAdapter.bondedDevices.firstOrNull { device ->
            device.name == config.deviceName || device.address == config.fallbackAddress
        } ?: bluetoothAdapter.getRemoteDevice(config.fallbackAddress)
    }.getOrNull()

    @SuppressLint("MissingPermission")
    private fun cancelDiscoveryIfAllowed(bluetoothAdapter: BluetoothAdapter) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ControlPermissions.hasRequiredPermissions(appContext)) {
            bluetoothAdapter.cancelDiscovery()
        }
    }

    private fun startReader(activeSocket: BluetoothSocket) {
        readJob?.cancel()
        readJob = scope.launch {
            val buffer = ByteArray(64)
            val rollingResponse = StringBuilder()

            try {
                while (activeSocket.isConnected) {
                    val bytesRead = activeSocket.inputStream.read(buffer)
                    if (bytesRead < 0) break

                    val chunk = String(buffer, 0, bytesRead, StandardCharsets.US_ASCII)
                    rollingResponse.append(chunk)
                    if (rollingResponse.length > MAX_RESPONSE_BUFFER) {
                        rollingResponse.delete(0, rollingResponse.length - MAX_RESPONSE_BUFFER)
                    }

                    val raw = rollingResponse.toString()
                    val state = ArduinoResponseParser.lastWifiState(raw)
                    if (state != null) {
                        _events.emit(ArduinoBluetoothEvent.WifiState(state, raw))
                    } else {
                        _events.emit(ArduinoBluetoothEvent.Response(chunk))
                    }
                }
            } catch (exception: IOException) {
                if (!disconnectRequested) {
                    _events.emit(ArduinoBluetoothEvent.Error("Connexion Bluetooth interrompue"))
                }
            } finally {
                if (!disconnectRequested) {
                    closeSocketOnly()
                    _events.emit(ArduinoBluetoothEvent.Disconnected)
                }
            }
        }
    }

    private fun closeSocketOnly() {
        readJob?.cancel()
        readJob = null
        runCatching { socket?.close() }
        socket = null
    }

    private companion object {
        const val MAX_RESPONSE_BUFFER = 128
    }
}

class BluetoothControlException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
