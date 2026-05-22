package ch.bus.roadpanel.feature.dashboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.bus.roadpanel.core.network.NetworkModule
import ch.bus.roadpanel.feature.energy.data.VictronHealthDto
import ch.bus.roadpanel.feature.energy.data.VictronMetricsDto
import ch.bus.roadpanel.feature.energy.domain.VictronRepository
import ch.bus.roadpanel.feature.energy.ui.EnergySummaryCard
import ch.bus.roadpanel.feature.energy.ui.EnergyUiState
import ch.bus.roadpanel.feature.energy.ui.EnergyViewModel
import ch.bus.roadpanel.feature.gps.domain.GpsRepository
import ch.bus.roadpanel.feature.gps.ui.GpsReading
import ch.bus.roadpanel.feature.gps.ui.GpsUiState
import ch.bus.roadpanel.feature.gps.ui.GpsViewModel
import ch.bus.roadpanel.ui.components.DashboardSection
import ch.bus.roadpanel.ui.components.MetricValue
import ch.bus.roadpanel.ui.components.RoadPanelCard
import ch.bus.roadpanel.ui.components.RoadPanelIconKind
import ch.bus.roadpanel.ui.components.StatusPill
import ch.bus.roadpanel.ui.components.TelemetryCard
import ch.bus.roadpanel.ui.theme.RoadPanelAccent
import ch.bus.roadpanel.ui.theme.RoadPanelCanvas
import ch.bus.roadpanel.ui.theme.RoadPanelError
import ch.bus.roadpanel.ui.theme.RoadPanelMuted
import ch.bus.roadpanel.ui.theme.RoadPanelSky
import ch.bus.roadpanel.ui.theme.RoadPanelSurfaceSoft
import ch.bus.roadpanel.ui.theme.RoadPanelTheme
import java.util.Locale

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val gpsViewModel: GpsViewModel = viewModel(
        factory = GpsViewModel.factory(GpsRepository(NetworkModule.gpsApi)),
    )
    val energyViewModel: EnergyViewModel = viewModel(
        factory = EnergyViewModel.factory(VictronRepository(NetworkModule.victronApi)),
    )
    val gpsState by gpsViewModel.uiState.collectAsState()
    val energyState by energyViewModel.uiState.collectAsState()

    DashboardContent(
        gpsState = gpsState,
        energyState = energyState,
        onEnergyRefresh = energyViewModel::refresh,
        modifier = modifier,
    )
}

@Composable
private fun DashboardContent(
    gpsState: GpsUiState,
    energyState: EnergyUiState,
    onEnergyRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RoadPanelCanvas),
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(420)) + slideInVertically(
                animationSpec = tween(420),
                initialOffsetY = { it / 8 },
            ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 22.dp,
                    top = 28.dp,
                    end = 22.dp,
                    bottom = 126.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item { DashboardHeader(state = gpsState) }
                item { CompactPositionCard(state = gpsState) }
                item {
                    EnergySummaryCard(
                        state = energyState,
                        onRefresh = onEnergyRefresh,
                    )
                }
                item {
                    DashboardSection(title = "Vehicle telemetry") {
                        TelemetryGrid(
                            items = listOf(
                                DashboardMetric("GPS", gpsStatusLabel(gpsState), null, gpsDetailLabel(gpsState), RoadPanelIconKind.Gps, RoadPanelSky),
                                DashboardMetric("Speed", gpsState.data?.speed?.format(1) ?: "--", "km/h", "Live navigation stream", RoadPanelIconKind.Speed, RoadPanelAccent),
                                DashboardMetric("Altitude", gpsState.data?.altitude?.format(0) ?: "--", "m", "Latest GPS altitude", RoadPanelIconKind.Altitude, RoadPanelSky),
                                DashboardMetric("Connection", connectionValue(gpsState), null, connectionDetail(gpsState), RoadPanelIconKind.Connection, connectionColor(gpsState)),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(state: GpsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatusPill(
            text = connectionValue(state),
            color = connectionColor(state),
        )
        Text(
            text = "RoadPanel",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Van telemetry, navigation and off-grid systems at a glance.",
            style = MaterialTheme.typography.bodyLarge,
            color = RoadPanelMuted,
        )
    }
}

@Composable
private fun CompactPositionCard(state: GpsUiState) {
    RoadPanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Current position",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = state.data?.time ?: "Waiting for GPS",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoadPanelMuted,
                    )
                }
                StatusPill(
                    text = gpsStatusLabel(state),
                    color = connectionColor(state),
                )
            }
        }
    }
}

@Composable
private fun PositionMetric(
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
            modifier = Modifier.padding(13.dp),
            label = label,
            value = value,
            unit = unit,
        )
    }
}

@Composable
private fun TelemetryGrid(items: List<DashboardMetric>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                rowItems.forEach { item ->
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        title = item.title,
                        value = item.value,
                        unit = item.unit,
                        status = item.status,
                        icon = item.icon,
                        accent = item.accent,
                    )
                }
                if (rowItems.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class DashboardMetric(
    val title: String,
    val value: String,
    val unit: String?,
    val status: String,
    val icon: RoadPanelIconKind,
    val accent: androidx.compose.ui.graphics.Color,
)

private fun gpsStatusLabel(state: GpsUiState): String = when {
    state.data != null -> "Locked"
    state.isLoading -> "Searching"
    state.error != null -> "Offline"
    else -> "Standby"
}

private fun gpsDetailLabel(state: GpsUiState): String = when {
    state.data != null -> "Updated at ${state.data.time}"
    state.isLoading -> "Querying vehicle GPS stream"
    state.error != null -> state.error
    else -> "Ready"
}

private fun connectionValue(state: GpsUiState): String = when {
    state.error != null -> "Offline"
    state.isLoading -> "Syncing"
    else -> "Online"
}

private fun connectionDetail(state: GpsUiState): String = when {
    state.error != null -> "Vehicle access point unavailable"
    state.isLoading -> "Refreshing telemetry"
    else -> "GPS API link operational"
}

private fun connectionColor(state: GpsUiState) = if (state.error == null) RoadPanelAccent else RoadPanelError

private fun Double.format(decimals: Int): String = "%.${decimals}f".format(Locale.US, this)

@Preview(showBackground = true)
@Composable
private fun DashboardContentPreview() {
    RoadPanelTheme {
        DashboardContent(
            gpsState = GpsUiState(
                data = GpsReading(
                    mapLatitude = 46.5197,
                    mapLongitude = 6.6323,
                    altitude = 481.0,
                    speed = 48.2,
                    track = 144.0,
                    time = "20:42",
                ),
            ),
            energyState = EnergyUiState(
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
            onEnergyRefresh = {},
        )
    }
}
