package ch.bus.roadpanel.feature.energy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.bus.roadpanel.core.network.NetworkModule
import ch.bus.roadpanel.feature.energy.data.VictronHealthDto
import ch.bus.roadpanel.feature.energy.data.VictronMetricsDto
import ch.bus.roadpanel.feature.energy.domain.VictronRepository
import ch.bus.roadpanel.ui.components.ConnectionStatusChip
import ch.bus.roadpanel.ui.components.DashboardSection
import ch.bus.roadpanel.ui.components.MetricValue
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
import ch.bus.roadpanel.ui.theme.RoadPanelSolar
import ch.bus.roadpanel.ui.theme.RoadPanelSurface
import ch.bus.roadpanel.ui.theme.RoadPanelSurfaceSoft
import ch.bus.roadpanel.ui.theme.RoadPanelTheme
import ch.bus.roadpanel.ui.theme.RoadPanelWarning
import java.util.Locale

@Composable
fun EnergyScreen(modifier: Modifier = Modifier) {
    val viewModel: EnergyViewModel = viewModel(
        factory = EnergyViewModel.factory(VictronRepository(NetworkModule.victronApi)),
    )
    val state by viewModel.uiState.collectAsState()

    EnergyContent(
        state = state,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
private fun EnergyContent(
    state: EnergyUiState,
    onRefresh: () -> Unit,
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
            item {
                EnergyHeader(
                    state = state,
                    onRefresh = onRefresh,
                )
            }

            item {
                when {
                    state.isLoading && state.metrics == null -> LoadingEnergyCard()
                    state.errorMessage != null -> EnergyErrorCard(
                        message = state.errorMessage,
                        onRetry = onRefresh,
                    )
                    state.waitingForMqttData -> WaitingForMqttDataCard(onRetry = onRefresh)
                    state.metrics == null -> EmptyEnergyCard(onRetry = onRefresh)
                    else -> SolarInputCard(state = state)
                }
            }

            if (state.metrics != null) {
                item {
                    EnergyMetricGrid(
                        state = state,
                        onRefresh = onRefresh,
                    )
                }
            }

            item {
                ConnectionCard(state = state)
            }
        }
    }
}

@Composable
private fun EnergyHeader(
    state: EnergyUiState,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    text = energyStatusText(state),
                    color = energyStatusColor(state),
                )
                Text(
                    text = "Énergie",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            RefreshButton(onRefresh = onRefresh)
        }
        Text(
            text = state.lastUpdated ?: state.metrics?.timestamp ?: "Les données MPPT s'actualisent toutes les 5 secondes",
            style = MaterialTheme.typography.bodyMedium,
            color = RoadPanelMuted,
        )
    }
}

@Composable
private fun RefreshButton(onRefresh: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .size(52.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onRefresh,
            ),
        shape = CircleShape,
        color = RoadPanelSurface,
        contentColor = RoadPanelAccent,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
    ) {
        RoadPanelIcon(
            kind = RoadPanelIconKind.Refresh,
            modifier = Modifier
                .padding(14.dp)
                .size(24.dp),
            color = RoadPanelAccent,
        )
    }
}

@Composable
private fun SolarInputCard(state: EnergyUiState) {
    val metrics = state.metrics ?: return
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
                        text = "Production solaire",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Production MPPT en direct",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoadPanelMuted,
                    )
                }
                IconBubble(
                    icon = RoadPanelIconKind.Solar,
                    accent = RoadPanelSolar,
                    size = 52,
                )
            }

            MetricValue(
                label = "Puissance",
                value = metrics.solarPower.format(0),
                unit = "W",
                large = true,
            )
        }
    }
}

@Composable
private fun EnergyMetricGrid(
    state: EnergyUiState,
    onRefresh: () -> Unit,
) {
    val metrics = state.metrics ?: return
    DashboardSection(title = "Aperçu MPPT") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                BatteryCard(
                    modifier = Modifier.weight(1f),
                    voltage = metrics.batteryVoltage,
                    current = metrics.batteryChargingCurrent,
                )
                CompactMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Production du jour",
                    label = "Récolté",
                    value = metrics.yieldToday.format(0),
                    unit = "Wh",
                    icon = RoadPanelIconKind.Solar,
                    accent = RoadPanelSolar,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CompactMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "État de charge",
                    label = "Contrôleur",
                    value = metrics.chargeState.toChargeStateLabel(),
                    icon = RoadPanelIconKind.Power,
                    accent = RoadPanelAccent,
                )
                ManualRefreshCard(
                    modifier = Modifier.weight(1f),
                    onRefresh = onRefresh,
                )
            }
        }
    }
}

@Composable
private fun BatteryCard(
    voltage: Double,
    current: Double,
    modifier: Modifier = Modifier,
) {
    RoadPanelCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Batterie",
                    style = MaterialTheme.typography.titleMedium,
                )
                IconBubble(
                    icon = RoadPanelIconKind.Battery,
                    accent = RoadPanelAccent,
                    size = 38,
                )
            }
            MetricValue(
                label = "Tension",
                value = voltage.format(1),
                unit = "V",
            )
            MetricValue(
                label = "Courant de charge",
                value = current.format(1),
                unit = "A",
            )
        }
    }
}

@Composable
private fun CompactMetricCard(
    title: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    icon: RoadPanelIconKind,
    accent: Color,
) {
    RoadPanelCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                IconBubble(
                    icon = icon,
                    accent = accent,
                    size = 38,
                )
            }
            MetricValue(
                label = label,
                value = value,
                unit = unit,
            )
        }
    }
}

@Composable
private fun ManualRefreshCard(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    RoadPanelCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onRefresh,
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            IconBubble(
                icon = RoadPanelIconKind.Refresh,
                accent = RoadPanelSky,
                size = 38,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Actualiser",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Mise à jour manuelle",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoadPanelMuted,
                )
            }
        }
    }
}

@Composable
private fun ConnectionCard(state: EnergyUiState) {
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
                    text = "Connexion",
                    style = MaterialTheme.typography.titleMedium,
                )
                ConnectionStatusChip(connected = state.health?.mqttConnected)
            }
            Text(
                text = state.health?.lastMessageTimestamp ?: "Aucun message MPPT reçu pour l'instant",
                style = MaterialTheme.typography.bodyMedium,
                color = RoadPanelMuted,
            )
        }
    }
}

@Composable
fun EnergySummaryCard(
    state: EnergyUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
) {
    RoadPanelCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Énergie",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = summarySubtitle(state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoadPanelMuted,
                    )
                }
                ConnectionStatusChip(connected = state.health?.mqttConnected)
            }

            if (state.errorMessage != null) {
                InlineAction(
                    text = "Victron indisponible",
                    detail = state.errorMessage,
                    color = RoadPanelError,
                    onClick = onRefresh,
                )
            } else if (state.waitingForMqttData) {
                InlineAction(
                    text = "En attente des données MPPT...",
                    detail = "Touchez pour réessayer",
                    color = RoadPanelWarning,
                    onClick = onRefresh,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        label = "Batterie",
                        value = state.metrics?.batteryVoltage?.format(1) ?: "--",
                        unit = "V",
                    )
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        label = "Solaire",
                        value = state.metrics?.solarPower?.format(0) ?: "--",
                        unit = "W",
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        label = "Production",
                        value = state.metrics?.yieldToday?.format(0) ?: "--",
                        unit = "Wh",
                    )
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        label = "État",
                        value = state.metrics?.chargeState?.toChargeStateLabel() ?: "--",
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = RoadPanelSurfaceSoft,
        tonalElevation = 0.dp,
    ) {
        MetricValue(
            modifier = Modifier.padding(14.dp),
            label = label,
            value = value,
            unit = unit,
        )
    }
}

@Composable
private fun LoadingEnergyCard() {
    RoadPanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(42.dp),
                color = RoadPanelAccent,
                strokeWidth = 4.dp,
            )
            Text(
                text = "Lecture de la télémétrie MPPT...",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Les mesures Victron apparaîtront dès que le Raspberry Pi répondra.",
                style = MaterialTheme.typography.bodyMedium,
                color = RoadPanelMuted,
            )
        }
    }
}

@Composable
private fun EnergyErrorCard(
    message: String,
    onRetry: () -> Unit,
) {
    StateCard(
        title = "Données énergie indisponibles",
        detail = message,
        color = RoadPanelError,
        buttonText = "Réessayer",
        onClick = onRetry,
    )
}

@Composable
private fun WaitingForMqttDataCard(onRetry: () -> Unit) {
    StateCard(
        title = "En attente des données MPPT...",
        detail = "L'API Victron est en ligne, mais aucune mesure MQTT valide n'est encore arrivée.",
        color = RoadPanelWarning,
        buttonText = "Actualiser",
        onClick = onRetry,
    )
}

@Composable
private fun EmptyEnergyCard(onRetry: () -> Unit) {
    StateCard(
        title = "Aucune mesure d'énergie pour le moment",
        detail = "Touchez Actualiser pour interroger le point Victron.",
        color = RoadPanelSky,
        buttonText = "Actualiser",
        onClick = onRetry,
    )
}

@Composable
private fun StateCard(
    title: String,
    detail: String,
    color: Color,
    buttonText: String,
    onClick: () -> Unit,
) {
    RoadPanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBubble(
                icon = RoadPanelIconKind.Connection,
                accent = color,
                size = 46,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = RoadPanelMuted,
            )
            InlineAction(
                text = buttonText,
                detail = "Requête manuelle",
                color = color,
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun InlineAction(
    text: String,
    detail: String,
    color: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.10f),
        contentColor = color,
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
                    color = color,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelMedium,
                    color = RoadPanelMuted,
                )
            }
            RoadPanelIcon(
                kind = RoadPanelIconKind.Refresh,
                modifier = Modifier.size(18.dp),
                color = color,
            )
        }
    }
}

@Composable
private fun IconBubble(
    icon: RoadPanelIconKind,
    accent: Color,
    size: Int,
) {
    Surface(
        modifier = Modifier.size(size.dp),
        shape = CircleShape,
        color = accent.copy(alpha = 0.12f),
        contentColor = accent,
        tonalElevation = 0.dp,
    ) {
        RoadPanelIcon(
            kind = icon,
            modifier = Modifier
                .padding((size * 0.24f).dp)
                .size((size * 0.52f).dp),
            color = accent,
        )
    }
}

private fun energyStatusText(state: EnergyUiState): String = when {
    state.errorMessage != null -> "Hors ligne"
    state.waitingForMqttData -> "En attente"
    state.isLoading -> "Synchronisation"
    state.metrics != null -> "En direct"
    else -> "Inactif"
}

private fun energyStatusColor(state: EnergyUiState): Color = when {
    state.errorMessage != null -> RoadPanelError
    state.waitingForMqttData -> RoadPanelWarning
    state.metrics != null -> RoadPanelAccent
    else -> RoadPanelMuted
}

private fun summarySubtitle(state: EnergyUiState): String = when {
    state.waitingForMqttData -> "En attente des données MPPT"
    state.errorMessage != null -> "Point Victron hors ligne"
    state.metrics != null -> "${state.metrics.solarPower.format(0)} W solaires maintenant"
    state.isLoading -> "Synchronisation MPPT"
    else -> "Aucune donnée pour le moment"
}

private fun String.toChargeStateLabel(): String {
    val normalized = lowercase(Locale.US)
    val translated = when (normalized) {
        "off" -> "Arrêt"
        "low_power", "low-power", "low power" -> "Puissance faible"
        "fault" -> "Défaut"
        "bulk" -> "Charge principale"
        "absorption" -> "Absorption"
        "float" -> "Maintien"
        "storage" -> "Stockage"
        "equalize", "equalization" -> "Égalisation"
        else -> null
    }
    if (translated != null) return translated

    return split("_", "-", " ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.lowercase(Locale.US).replaceFirstChar { char -> char.titlecase(Locale.US) }
        }
}

private fun Double.format(decimals: Int): String = "%.${decimals}f".format(Locale.US, this)

@Preview(showBackground = true)
@Composable
private fun EnergyContentPreview() {
    RoadPanelTheme {
        EnergyContent(
            state = EnergyUiState(
                health = VictronHealthDto(
                    status = "ok",
                    mqttConnected = true,
                    lastMessageTimestamp = "2026-05-22T08:32:11.912940+00:00",
                ),
                metrics = VictronMetricsDto(
                    timestamp = "2026-05-22T08:32:11.912940+00:00",
                    batteryChargingCurrent = 0.6,
                    batteryVoltage = 12.6,
                    chargeState = "bulk",
                    solarPower = 8.0,
                    yieldToday = 20.0,
                ),
                lastUpdated = "2026-05-22T08:32:11.912940+00:00",
            ),
            onRefresh = {},
        )
    }
}
