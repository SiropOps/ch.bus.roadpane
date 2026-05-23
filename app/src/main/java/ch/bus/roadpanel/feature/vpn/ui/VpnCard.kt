package ch.bus.roadpanel.feature.vpn.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.bus.roadpanel.feature.vpn.data.OpenVpnController
import ch.bus.roadpanel.feature.vpn.domain.VanConnectivityStatus
import ch.bus.roadpanel.ui.components.RoadPanelCard
import ch.bus.roadpanel.ui.components.RoadPanelIcon
import ch.bus.roadpanel.ui.components.RoadPanelIconKind
import ch.bus.roadpanel.ui.components.StatusPill
import ch.bus.roadpanel.ui.theme.RoadPanelAccent
import ch.bus.roadpanel.ui.theme.RoadPanelError
import ch.bus.roadpanel.ui.theme.RoadPanelMuted
import ch.bus.roadpanel.ui.theme.RoadPanelSky
import ch.bus.roadpanel.ui.theme.RoadPanelSurfaceSoft
import ch.bus.roadpanel.ui.theme.RoadPanelWarning

@Composable
fun VanVpnCard(
    state: VpnUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenOpenVpn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RoadPanelCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Van VPN",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Profil : ${OpenVpnController.VAN_PROFILE_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoadPanelMuted,
                    )
                }
                StatusPill(
                    text = if (state.openVpnInstalled) "Installed" else "Not installed",
                    color = if (state.openVpnInstalled) RoadPanelAccent else RoadPanelError,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusLine(
                    label = "Van link",
                    value = vpnStatusLabel(state),
                    color = vpnStatusColor(state),
                )
                StatusPill(
                    text = vpnStatusLabel(state),
                    color = vpnStatusColor(state),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VpnAction(
                    text = if (state.isLaunchingOpenVpn) "Opening..." else "Connect",
                    detail = "Lancer le profil OpenVPN du van",
                    color = RoadPanelAccent,
                    enabled = state.openVpnInstalled && !state.isLaunchingOpenVpn,
                    onClick = onConnect,
                )
                VpnAction(
                    text = "Disconnect",
                    detail = "Demander l'arret du profil",
                    color = RoadPanelSky,
                    enabled = state.openVpnInstalled && !state.isLaunchingOpenVpn,
                    onClick = onDisconnect,
                )
                VpnAction(
                    text = "Open OpenVPN",
                    detail = "Verifier profil, autorisations et confirmations",
                    color = RoadPanelWarning,
                    enabled = !state.isLaunchingOpenVpn,
                    onClick = onOpenOpenVpn,
                )
            }

            state.message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = RoadPanelMuted,
                )
            }
        }
    }
}

@Composable
fun VanStatusChip(
    status: VanConnectivityStatus,
    isChecking: Boolean,
    modifier: Modifier = Modifier,
) {
    val label = when {
        isChecking && status == VanConnectivityStatus.UNKNOWN -> "Van check"
        status == VanConnectivityStatus.ONLINE -> "Van online"
        status == VanConnectivityStatus.OFFLINE -> "Van offline"
        else -> "Van unknown"
    }

    StatusPill(
        modifier = modifier,
        text = label,
        color = when (status) {
            VanConnectivityStatus.ONLINE -> RoadPanelAccent
            VanConnectivityStatus.OFFLINE -> RoadPanelError
            VanConnectivityStatus.UNKNOWN -> RoadPanelMuted
        },
    )
}

@Composable
fun ConnectVpnChip(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        ),
        shape = RoundedCornerShape(999.dp),
        color = if (enabled) RoadPanelAccent.copy(alpha = 0.12f) else RoadPanelSurfaceSoft,
        contentColor = if (enabled) RoadPanelAccent else RoadPanelMuted,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoadPanelIcon(
                kind = RoadPanelIconKind.Connection,
                modifier = Modifier.size(15.dp),
                color = if (enabled) RoadPanelAccent else RoadPanelMuted,
            )
            Text(
                text = "Connect VPN",
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) RoadPanelAccent else RoadPanelMuted,
            )
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = RoadPanelMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun VpnAction(
    text: String,
    detail: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(18.dp),
        color = if (enabled) color.copy(alpha = 0.10f) else RoadPanelSurfaceSoft,
        contentColor = if (enabled) color else RoadPanelMuted,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) color else RoadPanelMuted,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelMedium,
                    color = RoadPanelMuted,
                )
            }
            RoadPanelIcon(
                kind = RoadPanelIconKind.Connection,
                modifier = Modifier.size(18.dp),
                color = if (enabled) color else RoadPanelMuted,
            )
        }
    }
}

private fun vpnStatusLabel(state: VpnUiState): String =
    when (state.vanStatus) {
        VanConnectivityStatus.ONLINE -> "Connected"
        VanConnectivityStatus.OFFLINE -> "Offline"
        VanConnectivityStatus.UNKNOWN -> "Unknown"
    }

private fun vpnStatusColor(state: VpnUiState): Color =
    when (state.vanStatus) {
        VanConnectivityStatus.ONLINE -> RoadPanelAccent
        VanConnectivityStatus.OFFLINE -> RoadPanelError
        VanConnectivityStatus.UNKNOWN -> RoadPanelMuted
    }
