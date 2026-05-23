package ch.bus.roadpanel.feature.vpn.ui

import ch.bus.roadpanel.feature.vpn.domain.VanConnectivityStatus

data class VpnUiState(
    val openVpnInstalled: Boolean = false,
    val vanStatus: VanConnectivityStatus = VanConnectivityStatus.UNKNOWN,
    val isCheckingConnectivity: Boolean = false,
    val isLaunchingOpenVpn: Boolean = false,
    val message: String? = null,
)
