package ch.bus.roadpanel.feature.control.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.bus.roadpanel.feature.control.domain.ArduinoBluetoothController
import ch.bus.roadpanel.feature.control.domain.ArduinoBluetoothEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ControlViewModel(
    private val controller: ArduinoBluetoothController,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    private var pendingEcho: String? = null
    private var echoTimeoutJob: Job? = null

    init {
        refreshDeviceState()
        observeController()
    }

    fun refreshDeviceState() {
        _uiState.update {
            it.copy(
                isBluetoothAvailable = controller.isBluetoothAvailable(),
                isBluetoothEnabled = controller.isBluetoothEnabled(),
                isPermissionGranted = controller.hasRequiredPermissions(),
            )
        }
    }

    fun connect() {
        refreshDeviceState()
        val state = _uiState.value
        when {
            !state.isBluetoothAvailable -> showError("Le Bluetooth n'est pas disponible sur cet appareil")
            !state.isPermissionGranted -> showError("La permission Bluetooth est manquante")
            !state.isBluetoothEnabled -> showError("Le Bluetooth est désactivé")
            else -> viewModelScope.launch {
                _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
                runCatching { controller.connect() }
                    .onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                isConnecting = false,
                                isConnected = false,
                                errorMessage = exception.message ?: "Impossible de se connecter à SmartArduino",
                            )
                        }
                    }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            echoTimeoutJob?.cancel()
            pendingEcho = null
            runCatching { controller.disconnect() }
                .onFailure { exception -> showError(exception.message ?: "Impossible de déconnecter le Bluetooth") }
        }
    }

    fun setWifiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val code = if (enabled) "1" else "0"
            pendingEcho = code
            _uiState.update {
                it.copy(
                    wifiEnabled = enabled,
                    lastResponse = "Envoyé $code",
                    errorMessage = null,
                )
            }
            runCatching { controller.setWifiEnabled(enabled) }
                .onSuccess { startEchoTimeout(code) }
                .onFailure { exception ->
                    pendingEcho = null
                    _uiState.update {
                        it.copy(errorMessage = exception.message ?: "Impossible d'envoyer la commande Wi-Fi")
                    }
                }
        }
    }

    private fun observeController() {
        viewModelScope.launch {
            controller.events.collect { event ->
                when (event) {
                    ArduinoBluetoothEvent.Connected -> _uiState.update {
                        it.copy(
                            isConnected = true,
                            isConnecting = false,
                            errorMessage = null,
                            lastResponse = "Connecté à SmartArduino",
                        )
                    }
                    ArduinoBluetoothEvent.Disconnected -> _uiState.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = false,
                            lastResponse = "Déconnecté",
                        )
                    }
                    is ArduinoBluetoothEvent.WifiState -> {
                        val code = if (event.enabled) "1" else "0"
                        if (pendingEcho == code) {
                            pendingEcho = null
                            echoTimeoutJob?.cancel()
                        }
                        _uiState.update {
                            it.copy(
                                wifiEnabled = event.enabled,
                                lastResponse = event.rawResponse.cleanResponse(),
                                errorMessage = null,
                            )
                        }
                    }
                    is ArduinoBluetoothEvent.Response -> _uiState.update {
                        it.copy(lastResponse = event.rawResponse.cleanResponse())
                    }
                    is ArduinoBluetoothEvent.Error -> _uiState.update {
                        it.copy(errorMessage = event.message, isConnected = false, isConnecting = false)
                    }
                }
                refreshDeviceState()
            }
        }
    }

    private fun startEchoTimeout(expectedCode: String) {
        echoTimeoutJob?.cancel()
        echoTimeoutJob = viewModelScope.launch {
            delay(4_000)
            if (pendingEcho == expectedCode) {
                pendingEcho = null
                _uiState.update {
                    it.copy(errorMessage = "Aucune réponse Arduino avant l'expiration de la commande")
                }
            }
        }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message, isConnecting = false) }
    }

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }

    companion object {
        fun factory(controller: ArduinoBluetoothController): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T = ControlViewModel(controller) as T
            }
    }
}

private fun String.cleanResponse(): String = trim()
    .filter { it != '\r' && it != '\n' }
    .takeIf { it.isNotBlank() }
    ?: "(vide)"
