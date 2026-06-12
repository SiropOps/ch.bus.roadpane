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
        onWifiToggle = viewModel::setWifiEnabled,
        onVanShutdown = viewModel::shutdownVanServer,
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
    onWifiToggle: (Boolean) -> Unit,
    onVanShutdown: () -> Unit,
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
                top = 16.dp,
                end = 22.dp,
                bottom = roadPanelBottomBarContentPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { ControlHeader(state = state) }
            item {
                WifiControlCard(
                    state = state,
                    onRequestPermission = onRequestPermission,
                    onWifiToggle = onWifiToggle,
                )
            }
            item {
                VanServerShutdownCard(
                    state = state,
                    onShutdown = onVanShutdown,
                )
            }
            item {
                VanVpnCard(
                    state = vpnState,
                    onConnect = onVpnConnect,
                    onDisconnect = onVpnDisconnect,
                    onOpenOpenVpn = onOpenOpenVpn,
                )
            }
        }
    }
}

@Composable
private fun VanServerShutdownCard(
    state: ControlUiState,
    onShutdown: () -> Unit,
) {
    val canShutdown = state.vanServerReachable && !state.isVanShutdownBusy
    val accent = vanServerStatusColor(state)

    RoadPanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Serveur VAN",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = vanServerStatusText(state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent,
                    )
                }
                IconBubble(
                    icon = RoadPanelIconKind.Power,
                    accent = accent,
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = canShutdown,
                        onClick = onShutdown,
                    ),
                shape = RoundedCornerShape(22.dp),
                color = accent.copy(alpha = 0.12f),
                contentColor = accent,
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = vanServerButtonText(state),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                    Text(
                        text = if (state.vanServerReachable) "ON" else "OFF",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                    )
                }
            }

            state.vanShutdownErrorMessage?.let {
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
    onWifiToggle: (Boolean) -> Unit,
) {
    val canToggle = state.isBluetoothAvailable &&
        state.isBluetoothEnabled &&
        state.isPermissionGranted &&
        !state.isWifiBusy
    val accent = when {
        state.wifiCommandPhase == WifiCommandPhase.ERROR -> RoadPanelError
        state.wifiEnabled -> RoadPanelAccent
        else -> RoadPanelMuted
    }

    RoadPanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Wi-Fi du van",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = wifiStatusText(state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = wifiStatusColor(state),
                    )
                }
                IconBubble(
                    icon = RoadPanelIconKind.Connection,
                    accent = accent,
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = canToggle,
                        onClick = { onWifiToggle(!state.wifiEnabled) },
                    ),
                shape = RoundedCornerShape(22.dp),
                color = if (state.wifiEnabled) RoadPanelAccent.copy(alpha = 0.12f) else RoadPanelSurfaceSoft,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (state.wifiEnabled) "ON" else "OFF",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.wifiEnabled) RoadPanelAccent else MaterialTheme.colorScheme.onSurface,
                    )
                    Switch(
                        checked = state.wifiEnabled,
                        onCheckedChange = onWifiToggle,
                        enabled = canToggle,
                    )
                }
            }

            if (!state.isPermissionGranted) {
                CompactPermissionAction(onClick = onRequestPermission)
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
private fun CompactPermissionAction(
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(14.dp),
        color = RoadPanelWarning.copy(alpha = 0.10f),
        contentColor = RoadPanelWarning,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Autoriser le Bluetooth",
                style = MaterialTheme.typography.labelLarge,
                color = RoadPanelWarning,
            )
            RoadPanelIcon(
                kind = RoadPanelIconKind.Connection,
                modifier = Modifier.size(18.dp),
                color = RoadPanelWarning,
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
    state.wifiCommandPhase == WifiCommandPhase.CONNECTING -> "Connexion"
    state.wifiCommandPhase == WifiCommandPhase.SENDING -> "Envoi"
    state.wifiCommandPhase == WifiCommandPhase.RETRYING -> "Nouvel essai"
    state.wifiCommandPhase == WifiCommandPhase.ERROR -> "Erreur"
    else -> "Prêt"
}

private fun controlStatusColor(state: ControlUiState): Color = when {
    !state.isBluetoothAvailable || !state.isBluetoothEnabled -> RoadPanelError
    !state.isPermissionGranted -> RoadPanelWarning
    state.wifiCommandPhase == WifiCommandPhase.ERROR -> RoadPanelError
    state.wifiCommandPhase == WifiCommandPhase.SUCCESS -> RoadPanelAccent
    else -> RoadPanelMuted
}

private fun wifiStatusText(state: ControlUiState): String = when {
    !state.isBluetoothAvailable -> "Bluetooth indisponible"
    !state.isPermissionGranted -> "Permission requise"
    !state.isBluetoothEnabled -> "Bluetooth désactivé"
    state.wifiCommandPhase == WifiCommandPhase.CONNECTING -> "Connexion..."
    state.wifiCommandPhase == WifiCommandPhase.SENDING -> "Envoi..."
    state.wifiCommandPhase == WifiCommandPhase.RETRYING -> "Essai ${state.wifiAttempt}/${state.wifiMaxAttempts}"
    state.wifiCommandPhase == WifiCommandPhase.ERROR -> "Erreur"
    else -> "Prêt"
}

private fun wifiStatusColor(state: ControlUiState): Color = when {
    !state.isBluetoothAvailable || !state.isBluetoothEnabled -> RoadPanelError
    !state.isPermissionGranted -> RoadPanelWarning
    state.wifiCommandPhase == WifiCommandPhase.ERROR -> RoadPanelError
    state.isWifiBusy -> RoadPanelSky
    else -> RoadPanelMuted
}

private fun vanServerStatusText(state: ControlUiState): String = when (state.vanShutdownPhase) {
    VanShutdownPhase.CHECKING -> "Ping en cours..."
    VanShutdownPhase.ONLINE -> "En ligne"
    VanShutdownPhase.OFFLINE -> "Hors ligne"
    VanShutdownPhase.SENDING -> "Commande shutdown now..."
    VanShutdownPhase.WAITING_OFFLINE -> "Arret en cours, ping ${state.vanShutdownPingAttempt}"
    VanShutdownPhase.TIMEOUT -> "Repond encore apres 30 s"
    VanShutdownPhase.ERROR -> "Erreur SSH"
}

private fun vanServerButtonText(state: ControlUiState): String = when (state.vanShutdownPhase) {
    VanShutdownPhase.CHECKING -> "Verification"
    VanShutdownPhase.SENDING -> "Envoi"
    VanShutdownPhase.WAITING_OFFLINE -> "Attente arret"
    VanShutdownPhase.OFFLINE -> "Serveur eteint"
    else -> "Eteindre"
}

private fun vanServerStatusColor(state: ControlUiState): Color = when (state.vanShutdownPhase) {
    VanShutdownPhase.OFFLINE -> RoadPanelError
    VanShutdownPhase.ERROR -> RoadPanelError
    VanShutdownPhase.TIMEOUT -> RoadPanelWarning
    VanShutdownPhase.CHECKING,
    VanShutdownPhase.SENDING,
    VanShutdownPhase.WAITING_OFFLINE -> RoadPanelSky
    VanShutdownPhase.ONLINE -> RoadPanelAccent
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
            onWifiToggle = {},
            onVanShutdown = {},
            onVpnConnect = {},
            onVpnDisconnect = {},
            onOpenOpenVpn = {},
        )
    }
}
