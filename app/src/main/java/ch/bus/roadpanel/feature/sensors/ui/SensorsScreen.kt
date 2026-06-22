package ch.bus.roadpanel.feature.sensors.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import ch.bus.roadpanel.feature.sensors.data.SensorDto
import ch.bus.roadpanel.feature.sensors.data.SensorsResponseDto
import ch.bus.roadpanel.feature.sensors.domain.SensorsRepository
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
import ch.bus.roadpanel.utils.formatRelativeTime
import java.util.Locale

private data class SensorSpec(
    val id: String,
    val fallbackName: String,
    val location: String,
    val icon: RoadPanelIconKind,
    val accent: Color,
    val isWired: Boolean = false,
)

private val sensorSpecs = listOf(
    SensorSpec("fruit_storage", "Fruit Storage", "Dans la glacière", RoadPanelIconKind.SensorCooler, RoadPanelSky),
    SensorSpec("tete_used", "Tête used", "À hauteur de tête, près de la batterie", RoadPanelIconKind.SensorHead, RoadPanelAccent),
    SensorSpec("avalanche_toit", "Avalanche Toit", "Dans le toit relevable", RoadPanelIconKind.SensorRoof, RoadPanelWarning),
    SensorSpec("ca_pique", "Ça pique", "À l'extérieur", RoadPanelIconKind.SensorOutside, RoadPanelSolar),
    SensorSpec("dht22", "DHT22", "Sous les sièges, près du chauffage", RoadPanelIconKind.SensorHeater, RoadPanelError, isWired = true),
)

@Composable
fun SensorsScreen(modifier: Modifier = Modifier) {
    val sensorsViewModel: SensorsViewModel = viewModel(
        factory = SensorsViewModel.factory(SensorsRepository(NetworkModule.sensorsApi)),
    )
    val state by sensorsViewModel.uiState.collectAsState()
    SensorsContent(state = state, onRefresh = sensorsViewModel::refresh, modifier = modifier)
}

@Composable
private fun SensorsContent(
    state: SensorsUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(RoadPanelCanvas)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 22.dp,
                top = 16.dp,
                end = 22.dp,
                bottom = roadPanelBottomBarContentPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { SensorsHeader(state = state, onRefresh = onRefresh) }

            if (state.response == null) {
                item {
                    when {
                        state.isLoading -> LoadingCard()
                        else -> ErrorCard(onRetry = onRefresh)
                    }
                }
            } else {
                sensorSpecs.forEach { spec ->
                    item(key = spec.id) {
                        SensorCard(
                            spec = spec,
                            sensor = state.response.sensors[spec.id],
                            explicitlyMissing = spec.id in state.response.missingSensors,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorsHeader(state: SensorsUiState, onRefresh: () -> Unit) {
    val response = state.response
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    text = when {
                        state.errorMessage != null -> "Connexion perdue"
                        response == null -> "Synchronisation"
                        response.sensorCount == response.expectedSensorCount -> "${response.sensorCount} capteurs en ligne"
                        else -> "${response.sensorCount}/${response.expectedSensorCount} capteurs en ligne"
                    },
                    color = when {
                        state.errorMessage != null -> RoadPanelError
                        response == null -> RoadPanelMuted
                        response.sensorCount == response.expectedSensorCount -> RoadPanelAccent
                        else -> RoadPanelWarning
                    },
                )
                Text("Capteurs", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
            RefreshButton(onRefresh)
        }
        Text(
            text = response?.timestamp?.let { "Dernière réception ${formatRelativeTime(it)}" }
                ?: "Les mesures s'actualisent toutes les 5 secondes",
            style = MaterialTheme.typography.bodyMedium,
            color = RoadPanelMuted,
        )
    }
}

@Composable
private fun RefreshButton(onRefresh: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier.size(52.dp).clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onRefresh,
        ),
        shape = CircleShape,
        color = RoadPanelSurface,
        contentColor = RoadPanelAccent,
        shadowElevation = 8.dp,
    ) {
        RoadPanelIcon(
            kind = RoadPanelIconKind.Refresh,
            modifier = Modifier.padding(14.dp),
            color = RoadPanelAccent,
        )
    }
}

@Composable
private fun SensorCard(spec: SensorSpec, sensor: SensorDto?, explicitlyMissing: Boolean) {
    RoadPanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = spec.accent.copy(alpha = 0.12f),
                    contentColor = spec.accent,
                ) {
                    RoadPanelIcon(spec.icon, Modifier.padding(12.dp), spec.accent)
                }
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = sensor?.name ?: spec.fallbackName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(spec.location, style = MaterialTheme.typography.bodyMedium, color = RoadPanelMuted)
                }
            }

            if (sensor == null || explicitlyMissing) {
                MissingSensorContent()
            } else {
                Text(
                    text = sensor.receivedAt?.let { "Dernière mesure ${formatRelativeTime(it)}" }
                        ?: sensor.timestamp?.let { "Dernière mesure ${formatRelativeTime(it)}" }
                        ?: "Heure de la mesure indisponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoadPanelMuted,
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SensorMetric(
                        label = "Température",
                        value = sensor.temperature?.format(1) ?: "—",
                        unit = sensor.temperature?.let { "°C" },
                        modifier = Modifier.weight(1f),
                    )
                    SensorMetric(
                        label = "Humidité",
                        value = sensor.humidity?.format(1) ?: "—",
                        unit = sensor.humidity?.let { "%" },
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val comfort = comfortFor(sensor.temperature, sensor.humidity)
                    StatusPill(text = comfort.label, color = comfort.color)
                    BatteryIndicator(sensor = sensor, isWired = spec.isWired)
                }
            }
        }
    }
}

@Composable
private fun SensorMetric(label: String, value: String, unit: String?, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = RoadPanelSurfaceSoft) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = RoadPanelMuted)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                unit?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = RoadPanelMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryIndicator(sensor: SensorDto, isWired: Boolean) {
    val text = if (isWired) "Alimenté" else sensor.battery?.let { "$it %" }
        ?: sensor.batteryVoltage?.let { "${it.format(2)} V" }
        ?: "—"
    val color = when {
        isWired -> RoadPanelAccent
        sensor.battery == null -> RoadPanelMuted
        sensor.battery <= 20 -> RoadPanelError
        sensor.battery <= 40 -> RoadPanelWarning
        else -> RoadPanelAccent
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        RoadPanelIcon(RoadPanelIconKind.Battery, Modifier.size(18.dp), color)
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun MissingSensorContent() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = RoadPanelWarning.copy(alpha = 0.10f),
    ) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Capteur indisponible", style = MaterialTheme.typography.labelLarge, color = RoadPanelWarning)
            Text("Aucune mesure reçue pour le moment.", style = MaterialTheme.typography.bodyMedium, color = RoadPanelMuted)
        }
    }
}

@Composable
private fun LoadingCard() {
    RoadPanelCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = RoadPanelAccent, strokeWidth = 3.dp)
            Text("Lecture des capteurs…", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ErrorCard(onRetry: () -> Unit) {
    RoadPanelCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Capteurs injoignables", style = MaterialTheme.typography.titleMedium, color = RoadPanelError)
            Text(
                "Impossible de récupérer les mesures. Vérifiez la connexion au réseau du van.",
                style = MaterialTheme.typography.bodyMedium,
                color = RoadPanelMuted,
            )
            Text(
                "Réessayer",
                modifier = Modifier.clickable(onClick = onRetry).padding(vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = RoadPanelAccent,
            )
        }
    }
}

private data class Comfort(val label: String, val color: Color)

private fun comfortFor(temperature: Double?, humidity: Double?): Comfort = when {
    temperature == null -> Comfort("Confort inconnu", RoadPanelMuted)
    temperature < 16 -> Comfort("Froid", RoadPanelSky)
    temperature < 19 -> Comfort("Frais", RoadPanelSky)
    temperature > 28 -> Comfort("Très chaud", RoadPanelError)
    temperature > 25 -> Comfort("Chaud", RoadPanelWarning)
    humidity != null && humidity < 35 -> Comfort("Air sec", RoadPanelWarning)
    humidity != null && humidity > 65 -> Comfort("Air humide", RoadPanelWarning)
    humidity == null -> Comfort("Température agréable", RoadPanelAccent)
    else -> Comfort("Confortable", RoadPanelAccent)
}

private fun Double.format(decimals: Int): String = "%.${decimals}f".format(Locale.getDefault(), this)

@Preview(showBackground = true)
@Composable
private fun SensorsContentPreview() {
    RoadPanelTheme {
        SensorsContent(
            state = SensorsUiState(
                response = SensorsResponseDto(
                    timestamp = "2026-06-22T21:07:08.773125+00:00",
                    sensorCount = 3,
                    expectedSensorCount = 4,
                    missingSensors = listOf("tete_used"),
                    sensors = mapOf(
                        "fruit_storage" to SensorDto(
                            timestamp = "2026-06-22T21:07:08.751942+00:00",
                            name = "Fruit Storage",
                            address = null,
                            protocol = "inkbird",
                            rssi = -52,
                            model = null,
                            temperature = 8.6,
                            battery = 25,
                            humidity = null,
                            pressure = null,
                            dataFormat = null,
                            accelerationX = null,
                            accelerationY = null,
                            accelerationZ = null,
                            batteryVoltage = null,
                            buttonPressed = null,
                            txPower = null,
                            movementCounter = null,
                            measurementSequence = null,
                            sensorId = "fruit_storage",
                            receivedAt = "2026-06-22T21:07:08.754783+00:00",
                        ),
                    ),
                ),
            ),
            onRefresh = {},
        )
    }
}
