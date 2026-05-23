package ch.bus.roadpanel.feature.control.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.bus.roadpanel.feature.control.data.BluetoothArduinoController
import ch.bus.roadpanel.feature.control.data.ControlPermissions
import ch.bus.roadpanel.feature.vpn.ui.VanVpnCard
import ch.bus.roadpanel.feature.vpn.ui.VpnUiState
import ch.bus.roadpanel.feature.vpn.ui.VpnViewModel
import ch.bus.roadpanel.ui.components.RoadPanelCard
import ch.bus.roadpanel.ui.components.RoadPanelIcon
import ch.bus.roadpanel.ui.components.RoadPanelIconKind
import ch.bus.roadpanel.ui.components.StatusPill
import ch.bus.roadpanel.ui.components.roadPanelBottomBarContentPadding
import ch.bus.roadpanel.ui.theme.RoadPanelAccent
import ch.bus.roadpanel.ui.theme.RoadPanelCanvas
import ch.bus.roadpanel.ui.theme.RoadPanelError
import ch.bus.roadpanel.ui.theme.RoadPanelMuted
import ch.bus.roadpanel.ui.theme.RoadPanelSky
import ch.bus.roadpanel.ui.theme.RoadPanelSurfaceSoft
import ch.bus.roadpanel.ui.theme.RoadPanelTheme
import ch.bus.roadpanel.ui.theme.RoadPanelWarning

@Composable
fun ControlScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val controller = remember(appContext) { BluetoothArduinoController(appContext) }
    val viewModel: ControlViewModel = viewModel(factory = ControlViewModel.factory(controller))
    val vpnViewModel: VpnViewModel = viewModel(factory = VpnViewModel.factory(appContext))
    val state by viewModel.uiState.collectAsState()
    val vpnState by vpnViewModel.uiState.collectAsState()
    val permissions = remember { ControlPermissions.runtimePermissions() }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refreshDeviceState()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshDeviceState()
    }

    ControlContent(
        state = state,
        vpnState = vpnState,
        onRequestPermission = {
            if (permissions.isNotEmpty()) {
                permissionLauncher.launch(permissions)
            } else {
                viewModel.refreshDeviceState()
            }
        },
        onConnect = viewModel::connect,
        onDisconnect = viewModel::disconnect,
        onWifiToggle = viewModel::setWifiEnabled,
        onVpnConnect = vpnViewModel::connectVanVpn,
        onVpnDisconnect = vpnViewModel::disconnectVanVpn,
        onOpenOpenVpn = vpnViewModel::openOpenVpnApp,
        modifier = modifier,
    )
}

@Composable
private fun ControlContent(
    state: ControlUiState,
    vpnState: VpnUiState,
    onRequestPermission: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onWifiToggle: (Boolean) -> Unit,
    onVpnConnect: () -> Unit,
    onVpnDisconnect: () -> Unit,
    onOpenOpenVpn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RoadPanelCanvas),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 22.dp,
                top = 28.dp,
                end = 22.dp,
                bottom = roadPanelBottomBarContentPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { ControlHeader(state = state) }
            item {
                VanVpnCard(
                    state = vpnState,
                    onConnect = onVpnConnect,
                    onDisconnect = onVpnDisconnect,
                    onOpenOpenVpn = onOpenOpenVpn,
                )
            }
            item {
                WifiControlCard(
                    state = state,
                    onRequestPermission = onRequestPermission,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                    onWifiToggle = onWifiToggle,
                )
            }
            item { BluetoothStatusCard(state = state) }
        }
    }
}

@Composable
private fun ControlHeader(state: ControlUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StatusPill(
            text = controlStatusText(state),
            color = controlStatusColor(state),
        )
        Text(
            text = "Commandes",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Liaison Bluetooth classique avec SmartArduino",
            style = MaterialTheme.typography.bodyMedium,
            color = RoadPanelMuted,
        )
    }
}

@Composable
private fun WifiControlCard(
    state: ControlUiState,
    onRequestPermission: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onWifiToggle: (Boolean) -> Unit,
) {
    RoadPanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Contrôle Wi-Fi",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = if (state.wifiEnabled) "Wi-Fi du van activé" else "Wi-Fi du van désactivé",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoadPanelMuted,
                    )
                }
                IconBubble(
                    icon = RoadPanelIconKind.Connection,
                    accent = if (state.wifiEnabled) RoadPanelAccent else RoadPanelMuted,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (state.wifiEnabled) "ACTIF" else "ARRÊT",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.wifiEnabled) RoadPanelAccent else MaterialTheme.colorScheme.onSurface,
                )
                Switch(
                    checked = state.wifiEnabled,
                    onCheckedChange = onWifiToggle,
                    enabled = state.isConnected,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!state.isPermissionGranted) {
                    ControlAction(
                        text = "Autoriser le Bluetooth",
                        detail = "Requis sur Android 12+",
                        color = RoadPanelWarning,
                        onClick = onRequestPermission,
                    )
                }
                if (!state.isConnected) {
                    ControlAction(
                        text = if (state.isConnecting) "Connexion..." else "Se connecter à SmartArduino",
                        detail = "FC:A8:9A:00:0D:9E",
                        color = RoadPanelAccent,
                        onClick = onConnect,
                        enabled = !state.isConnecting,
                    )
                } else {
                    ControlAction(
                        text = "Déconnecter",
                        detail = "Fermer la connexion RFCOMM",
                        color = RoadPanelSky,
                        onClick = onDisconnect,
                    )
                }
            }

            state.errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoadPanelError,
                )
            }
        }
    }
}

@Composable
private fun BluetoothStatusCard(state: ControlUiState) {
    RoadPanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "État Bluetooth",
                    style = MaterialTheme.typography.titleMedium,
                )
                StatusPill(
                    text = if (state.isConnected) "Connecté" else "Hors ligne",
                    color = if (state.isConnected) RoadPanelAccent else RoadPanelMuted,
                )
            }
            StatusLine(label = "Adaptateur", value = if (state.isBluetoothAvailable) "Disponible" else "Indisponible")
            StatusLine(label = "Bluetooth", value = if (state.isBluetoothEnabled) "Activé" else "Désactivé")
            StatusLine(label = "Permission", value = if (state.isPermissionGranted) "Accordée" else "Manquante")
            Text(
                text = "Dernière réponse Arduino : ${state.lastResponse ?: "--"}",
                style = MaterialTheme.typography.bodyMedium,
                color = RoadPanelMuted,
            )
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = RoadPanelMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ControlAction(
    text: String,
    detail: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
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

@Composable
private fun IconBubble(
    icon: RoadPanelIconKind,
    accent: Color,
) {
    Surface(
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        color = accent.copy(alpha = 0.12f),
        contentColor = accent,
        tonalElevation = 0.dp,
    ) {
        RoadPanelIcon(
            kind = icon,
            modifier = Modifier
                .padding(13.dp)
                .size(26.dp),
            color = accent,
        )
    }
}

private fun controlStatusText(state: ControlUiState): String = when {
    !state.isBluetoothAvailable -> "Aucun adaptateur"
    !state.isPermissionGranted -> "Permission"
    !state.isBluetoothEnabled -> "Désactivé"
    state.isConnecting -> "Connexion"
    state.isConnected -> "En ligne"
    else -> "Prêt"
}

private fun controlStatusColor(state: ControlUiState): Color = when {
    !state.isBluetoothAvailable || !state.isBluetoothEnabled -> RoadPanelError
    !state.isPermissionGranted -> RoadPanelWarning
    state.isConnected -> RoadPanelAccent
    else -> RoadPanelMuted
}

@Preview(showBackground = true)
@Composable
private fun ControlContentPreview() {
    RoadPanelTheme {
        ControlContent(
            state = ControlUiState(
                isBluetoothAvailable = true,
                isBluetoothEnabled = true,
                isPermissionGranted = true,
                isConnected = true,
                wifiEnabled = true,
                lastResponse = "1",
            ),
            vpnState = VpnUiState(openVpnInstalled = true),
            onRequestPermission = {},
            onConnect = {},
            onDisconnect = {},
            onWifiToggle = {},
            onVpnConnect = {},
            onVpnDisconnect = {},
            onOpenOpenVpn = {},
        )
    }
}
