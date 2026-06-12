package ch.bus.roadpanel.feature.control.ui

data class ControlUiState(
    val isBluetoothAvailable: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val wifiEnabled: Boolean = false,
    val wifiCommandPhase: WifiCommandPhase = WifiCommandPhase.IDLE,
    val wifiAttempt: Int = 0,
    val wifiMaxAttempts: Int = 5,
    val vanServerReachable: Boolean = false,
    val vanShutdownPhase: VanShutdownPhase = VanShutdownPhase.CHECKING,
    val vanShutdownPingAttempt: Int = 0,
    val vanShutdownErrorMessage: String? = null,
    val lastResponse: String? = null,
    val errorMessage: String? = null,
) {
    val isWifiBusy: Boolean
        get() = wifiCommandPhase == WifiCommandPhase.CONNECTING ||
            wifiCommandPhase == WifiCommandPhase.SENDING ||
            wifiCommandPhase == WifiCommandPhase.RETRYING

    val isVanShutdownBusy: Boolean
        get() = vanShutdownPhase == VanShutdownPhase.CHECKING ||
            vanShutdownPhase == VanShutdownPhase.SENDING ||
            vanShutdownPhase == VanShutdownPhase.WAITING_OFFLINE
}

enum class WifiCommandPhase {
    IDLE,
    CONNECTING,
    SENDING,
    RETRYING,
    SUCCESS,
    ERROR,
}

enum class VanShutdownPhase {
    CHECKING,
    ONLINE,
    OFFLINE,
    SENDING,
    WAITING_OFFLINE,
    TIMEOUT,
    ERROR,
}
