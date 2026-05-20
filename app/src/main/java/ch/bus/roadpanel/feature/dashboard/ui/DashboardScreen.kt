package ch.bus.roadpanel.feature.dashboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
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
import ch.bus.roadpanel.feature.gps.domain.GpsRepository
import ch.bus.roadpanel.feature.gps.ui.GpsReading
import ch.bus.roadpanel.feature.gps.ui.GpsUiState
import ch.bus.roadpanel.feature.gps.ui.GpsViewModel
import ch.bus.roadpanel.ui.components.DashboardSection
import ch.bus.roadpanel.ui.components.RoadPanelCard
import ch.bus.roadpanel.ui.components.RoadPanelIconKind
import ch.bus.roadpanel.ui.components.StatusPill
import ch.bus.roadpanel.ui.components.TelemetryCard
import ch.bus.roadpanel.ui.theme.RoadPanelAccent
import ch.bus.roadpanel.ui.theme.RoadPanelCanvas
import ch.bus.roadpanel.ui.theme.RoadPanelError
import ch.bus.roadpanel.ui.theme.RoadPanelMuted
import ch.bus.roadpanel.ui.theme.RoadPanelSky
import ch.bus.roadpanel.ui.theme.RoadPanelSolar
import ch.bus.roadpanel.ui.theme.RoadPanelTheme
import java.util.Locale

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val viewModel: GpsViewModel = viewModel(factory = GpsViewModel.factory(GpsRepository(NetworkModule.gpsApi)))
    val state by viewModel.uiState.collectAsState()
    DashboardContent(state = state, modifier = modifier)
}

@Composable
private fun DashboardContent(
    state: GpsUiState,
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 22.dp,
                    top = 28.dp,
                    end = 22.dp,
                    bottom = 126.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item { DashboardHeader(state = state) }
                item { RouteSummaryCard(state = state) }
                item {
                    DashboardSection(title = "Vehicle Telemetry") {
                        TelemetryGrid(
                            items = listOf(
                                DashboardMetric("Battery", "87", "%", "Aux bank estimated", RoadPanelIconKind.Battery, RoadPanelAccent),
                                DashboardMetric("GPS", gpsStatusLabel(state), null, gpsDetailLabel(state), RoadPanelIconKind.Gps, RoadPanelSky),
                                DashboardMetric("Speed", state.data?.speed?.format(1) ?: "--", "km/h", "Live navigation feed", RoadPanelIconKind.Speed, RoadPanelAccent),
                                DashboardMetric("Altitude", state.data?.altitude?.format(0) ?: "--", "m", "Barometric feed pending", RoadPanelIconKind.Altitude, RoadPanelSky),
                            ),
                        )
                    }
                }
                item {
                    DashboardSection(title = "Energy & Systems") {
                        TelemetryGrid(
                            items = listOf(
                                DashboardMetric("Solar", "124", "W", "Placeholder until solar API lands", RoadPanelIconKind.Solar, RoadPanelSolar),
                                DashboardMetric("Connection", connectionValue(state), null, connectionDetail(state), RoadPanelIconKind.Connection, connectionColor(state)),
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
            text = "Van telemetry, navigation, and off-grid systems at a glance.",
            style = MaterialTheme.typography.bodyLarge,
            color = RoadPanelMuted,
        )
    }
}

@Composable
private fun RouteSummaryCard(state: GpsUiState) {
    RoadPanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Current Position",
                        style = MaterialTheme.typography.labelLarge,
                        color = RoadPanelMuted,
                    )
                    Text(
                        text = state.data?.time ?: "Waiting for GPS",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                StatusPill(
                    text = gpsStatusLabel(state),
                    color = connectionColor(state),
                )
            }

            Text(
                text = state.data?.let {
                    "${it.mapLatitude.format(5)}, ${it.mapLongitude.format(5)}"
                } ?: "Map coordinates will appear once the GPS feed responds.",
                style = MaterialTheme.typography.bodyMedium,
                color = RoadPanelMuted,
            )
        }
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
    state.data != null -> "Fix updated at ${state.data.time}"
    state.isLoading -> "Polling vehicle feed"
    state.error != null -> state.error
    else -> "Ready"
}

private fun connectionValue(state: GpsUiState): String = when {
    state.error != null -> "Offline"
    state.isLoading -> "Syncing"
    else -> "Online"
}

private fun connectionDetail(state: GpsUiState): String = when {
    state.error != null -> "Vehicle endpoint unavailable"
    state.isLoading -> "Refreshing telemetry"
    else -> "API link healthy"
}

private fun connectionColor(state: GpsUiState) = if (state.error == null) RoadPanelAccent else RoadPanelError

private fun Double.format(decimals: Int): String = "%.${decimals}f".format(Locale.US, this)

@Preview(showBackground = true)
@Composable
private fun DashboardContentPreview() {
    RoadPanelTheme {
        DashboardContent(
            state = GpsUiState(
                data = GpsReading(
                    mapLatitude = 46.5197,
                    mapLongitude = 6.6323,
                    altitude = 481.0,
                    speed = 48.2,
                    track = 144.0,
                    time = "20:42",
                ),
            ),
        )
    }
}
