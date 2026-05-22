package ch.bus.roadpanel.feature.control.ui

data class ControlUiState(
    val isBluetoothAvailable: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val wifiEnabled: Boolean = false,
    val lastResponse: String? = null,
    val errorMessage: String? = null,
)
