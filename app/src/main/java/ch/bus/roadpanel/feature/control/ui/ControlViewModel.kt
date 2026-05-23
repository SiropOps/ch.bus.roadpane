package ch.bus.roadpanel.feature.control.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.bus.roadpanel.feature.control.domain.ArduinoBluetoothController
import ch.bus.roadpanel.feature.control.domain.ArduinoWifiCommandProgress
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

    init {
        refreshDeviceState()
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

    fun setWifiEnabled(enabled: Boolean) {
        refreshDeviceState()
        val state = _uiState.value
        when {
            state.isWifiBusy -> return
            !state.isBluetoothAvailable -> showError("Le Bluetooth n'est pas disponible sur cet appareil")
            !state.isPermissionGranted -> showError("La permission Bluetooth est manquante")
            !state.isBluetoothEnabled -> showError("Le Bluetooth est désactivé")
            else -> viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        wifiCommandPhase = WifiCommandPhase.CONNECTING,
                        wifiAttempt = 1,
                        errorMessage = null,
                        isConnecting = true,
                        isConnected = false,
                    )
                }

                runCatching {
                    controller.sendWifiCommand(enabled) { progress ->
                        _uiState.update { it.withProgress(progress) }
                    }
                }.onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            wifiEnabled = result.enabled,
                            wifiCommandPhase = WifiCommandPhase.SUCCESS,
                            wifiAttempt = result.attempts,
                            wifiMaxAttempts = result.attempts.coerceAtLeast(it.wifiMaxAttempts),
                            lastResponse = result.rawResponse.cleanResponse(),
                            errorMessage = null,
                            isConnected = false,
                            isConnecting = false,
                        )
                    }
                }.onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            wifiCommandPhase = WifiCommandPhase.ERROR,
                            errorMessage = exception.message ?: "Impossible de joindre SmartArduino",
                            isConnected = false,
                            isConnecting = false,
                        )
                    }
                }
                refreshDeviceState()
            }
        }
    }

    private fun ControlUiState.withProgress(progress: ArduinoWifiCommandProgress): ControlUiState {
        val phase = when (progress) {
            is ArduinoWifiCommandProgress.Connecting -> WifiCommandPhase.CONNECTING
            is ArduinoWifiCommandProgress.Sending -> WifiCommandPhase.SENDING
            is ArduinoWifiCommandProgress.Retrying -> WifiCommandPhase.RETRYING
        }
        return copy(
            wifiCommandPhase = phase,
            wifiAttempt = progress.attempt,
            wifiMaxAttempts = progress.maxAttempts,
            errorMessage = null,
            isConnecting = phase == WifiCommandPhase.CONNECTING || phase == WifiCommandPhase.RETRYING,
            isConnected = false,
        )
    }

    private fun showError(message: String) {
        _uiState.update {
            it.copy(
                wifiCommandPhase = WifiCommandPhase.ERROR,
                errorMessage = message,
                isConnecting = false,
                isConnected = false,
            )
        }
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
