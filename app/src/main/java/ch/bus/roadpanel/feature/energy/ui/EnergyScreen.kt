package ch.bus.roadpanel.feature.energy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.bus.roadpanel.core.network.NetworkModule
import ch.bus.roadpanel.feature.energy.data.VictronDeviceDto
import ch.bus.roadpanel.feature.energy.data.VictronHealthDto
import ch.bus.roadpanel.feature.energy.domain.VictronRepository
import ch.bus.roadpanel.ui.components.*
import ch.bus.roadpanel.ui.theme.*
import ch.bus.roadpanel.utils.formatRelativeTime
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun EnergyScreen(modifier: Modifier = Modifier) {
    val viewModel: EnergyViewModel = viewModel(factory = EnergyViewModel.factory(VictronRepository(NetworkModule.victronApi)))
    val state by viewModel.uiState.collectAsState()
    EnergyContent(state, viewModel::refresh, modifier)
}

@Composable
private fun EnergyContent(state: EnergyUiState, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    val devices = state.metrics.orEmpty().entries.sortedWith(
        compareBy<Map.Entry<String, VictronDeviceDto>> { it.value.deviceKind().order }.thenBy { it.key },
    )
    Box(modifier.fillMaxSize().background(RoadPanelCanvas)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(22.dp, 16.dp, 22.dp, roadPanelBottomBarContentPadding()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { EnergyHeader(state, onRefresh) }
            when {
                state.isLoading && state.metrics == null -> item { LoadingEnergyCard() }
                state.errorMessage != null && state.metrics == null -> item {
                    StateCard("Données énergie indisponibles", state.errorMessage, RoadPanelError, onRefresh)
                }
                state.waitingForMqttData -> item {
                    StateCard("En attente des appareils…", "L’API répond, mais aucune télémétrie n’est encore disponible.", RoadPanelWarning, onRefresh)
                }
                devices.isEmpty() -> item {
                    StateCard("Aucun appareil détecté", "Touchez Actualiser pour relire /api/metrics.", RoadPanelSky, onRefresh)
                }
                else -> items(devices, key = { it.key }) { (key, device) -> DeviceCard(key, device) }
            }
            if (devices.isNotEmpty()) item { ConnectionCard(state) }
        }
    }
}

@Composable
private fun EnergyHeader(state: EnergyUiState, onRefresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(text = energyStatusText(state), color = energyStatusColor(state))
                Text("Énergie", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
            RefreshButton(onRefresh)
        }
        val count = state.metrics?.size
        Text(
            listOfNotNull(
                count?.let { "$it appareil${if (it > 1) "s" else ""}" },
                state.lastUpdated?.let(::formatRelativeTime),
            ).joinToString(" · ").ifEmpty { "Actualisation automatique toutes les 5 secondes" },
            style = MaterialTheme.typography.bodyMedium,
            color = RoadPanelMuted,
        )
    }
}

@Composable
private fun RefreshButton(onRefresh: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier.size(52.dp).clickable(interaction, null, onClick = onRefresh),
        shape = CircleShape, color = RoadPanelSurface, shadowElevation = 8.dp,
    ) {
        RoadPanelIcon(RoadPanelIconKind.Refresh, Modifier.padding(14.dp).size(24.dp), RoadPanelAccent)
    }
}

@Composable
private fun DeviceCard(apiKey: String, device: VictronDeviceDto) {
    val kind = device.deviceKind()
    RoadPanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            DeviceHeader(apiKey, device, kind)
            when (kind) {
                DeviceKind.Solar -> SolarMetrics(device)
                DeviceKind.BatteryProtect -> BatteryProtectMetrics(device)
                DeviceKind.Inverter -> InverterMetrics(device)
                DeviceKind.Other -> GenericMetrics(device)
            }
            DeviceMetadata(device)
        }
    }
}

@Composable
private fun DeviceHeader(apiKey: String, device: VictronDeviceDto, kind: DeviceKind) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(kind.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                device.modelName ?: device.name ?: apiKey.toReadableLabel(),
                style = MaterialTheme.typography.bodyMedium, color = RoadPanelMuted,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(14.dp))
        IconBubble(kind.icon, kind.accent, 48)
    }
}

@Composable
private fun SolarMetrics(device: VictronDeviceDto) {
    device.solarPower?.let { HighlightMetric("Production solaire", it.format(0), "W", RoadPanelSolar) }
    MetricGrid(listOfNotNull(
        device.batteryVoltage?.metric("Tension batterie", "V", 2),
        device.batteryChargingCurrent?.metric("Courant de charge", "A", 1),
        device.yieldToday?.metric("Production du jour", "Wh", 0),
        device.chargeState?.let { Metric("Phase de charge", it.toReadableStatus()) },
        device.chargerError?.let { Metric("Erreur chargeur", it.toReadableStatus()) },
    ))
}

@Composable
private fun BatteryProtectMetrics(device: VictronDeviceDto) {
    val healthy = device.errorCode.isHealthyStatus() && device.alarmReason.isHealthyStatus()
    StatusBanner(
        device.outputState?.let { "Sortie ${it.toReadableStatus().lowercase(Locale.getDefault())}" }
            ?: device.deviceState?.toReadableStatus() ?: "État inconnu",
        if (healthy) RoadPanelAccent else RoadPanelError,
    )
    MetricGrid(listOfNotNull(
        device.inputVoltage?.metric("Tension d’entrée", "V", 2),
        device.outputVoltage?.metric("Tension de sortie", "V", 2),
        device.deviceState?.let { Metric("État appareil", it.toReadableStatus()) },
        device.outputState?.let { Metric("État sortie", it.toReadableStatus()) },
        device.alarmReason?.let { Metric("Alarme", it.toReadableStatus()) },
        device.warningReason?.let { Metric("Avertissement", it.toReadableStatus()) },
        device.errorCode?.let { Metric("Code erreur", it.toReadableStatus()) },
        device.offReason?.let { Metric("Cause d’arrêt", it.toReadableStatus()) },
    ))
}

@Composable
private fun InverterMetrics(device: VictronDeviceDto) {
    device.acApparentPower?.let { PowerGauge(it, device.inverterCapacityVa() ?: 500.0) }
    MetricGrid(listOfNotNull(
        device.acVoltage?.metric("Tension AC", "V", 2),
        device.acCurrent?.metric("Courant AC", "A", 1),
        device.batteryVoltage?.metric("Tension batterie", "V", 2),
        device.deviceState?.let { Metric("État appareil", it.toReadableStatus()) },
    ))
}

@Composable
private fun GenericMetrics(device: VictronDeviceDto) {
    MetricGrid(listOfNotNull(
        device.solarPower?.metric("Puissance solaire", "W", 0),
        device.acApparentPower?.metric("Puissance apparente", "VA", 0),
        device.batteryVoltage?.metric("Tension batterie", "V", 2),
        device.inputVoltage?.metric("Tension d’entrée", "V", 2),
        device.outputVoltage?.metric("Tension de sortie", "V", 2),
        device.deviceState?.let { Metric("État", it.toReadableStatus()) },
        device.errorCode?.let { Metric("Erreur", it.toReadableStatus()) },
    ))
}

@Composable
private fun PowerGauge(power: Double, capacity: Double) {
    val fraction = (power / capacity).coerceIn(0.0, 1.0).toFloat()
    val color = when { fraction >= .9f -> RoadPanelError; fraction >= .7f -> RoadPanelWarning; else -> RoadPanelAccent }
    Surface(shape = RoundedCornerShape(22.dp), color = color.copy(alpha = .08f)) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * .10f
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(RoadPanelMuted.copy(alpha = .18f), 150f, 240f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(color, 150f, 240f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(power.format(0), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("VA", style = MaterialTheme.typography.labelMedium, color = RoadPanelMuted)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Puissance onduleur", style = MaterialTheme.typography.titleMedium)
                Text("${(fraction * 100).roundToInt()} % de ${capacity.format(0)} VA", color = RoadPanelMuted)
                Text("Puissance apparente AC", style = MaterialTheme.typography.labelMedium, color = color)
            }
        }
    }
}

@Composable
private fun HighlightMetric(label: String, value: String, unit: String, color: Color) {
    Surface(shape = RoundedCornerShape(22.dp), color = color.copy(alpha = .09f)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            MetricValue(label = "En direct", value = value, unit = unit, large = true)
        }
    }
}

private data class Metric(val label: String, val value: String, val unit: String? = null)

@Composable
private fun MetricGrid(metrics: List<Metric>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowMetrics.forEach { MetricTile(it, Modifier.weight(1f)) }
                if (rowMetrics.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricTile(metric: Metric, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = RoadPanelSurfaceSoft) {
        MetricValue(
            label = metric.label,
            value = metric.value,
            modifier = Modifier.padding(14.dp),
            unit = metric.unit,
        )
    }
}

@Composable
private fun StatusBanner(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(14.dp), color = color.copy(alpha = .10f), contentColor = color) {
        Text(text, Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DeviceMetadata(device: VictronDeviceDto) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Surface(Modifier.fillMaxWidth().height(1.dp), color = RoadPanelMuted.copy(alpha = .12f)) {}
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(device.name?.toReadableLabel() ?: "Appareil Victron", style = MaterialTheme.typography.labelMedium, color = RoadPanelMuted)
            device.rssi?.let { Text("Signal $it dBm", style = MaterialTheme.typography.labelMedium, color = rssiColor(it)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(device.address ?: "Adresse inconnue", style = MaterialTheme.typography.labelSmall, color = RoadPanelMuted)
            Text(device.timestamp?.let(::formatRelativeTime) ?: "Horodatage inconnu", style = MaterialTheme.typography.labelSmall, color = RoadPanelMuted)
        }
    }
}

@Composable
private fun ConnectionCard(state: EnergyUiState) {
    RoadPanelCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(18.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Connexion VECTRON", style = MaterialTheme.typography.titleMedium)
                Text("${NetworkModule.VAN_HOST}:8013/api/metrics", style = MaterialTheme.typography.bodySmall, color = RoadPanelMuted)
            }
            ConnectionStatusChip(connected = state.health?.mqttConnected ?: (state.metrics != null))
        }
    }
}

@Composable
fun EnergySummaryCard(state: EnergyUiState, modifier: Modifier = Modifier, onRefresh: () -> Unit = {}) {
    val devices = state.metrics.orEmpty().values
    val solar = devices.firstOrNull { it.deviceKind() == DeviceKind.Solar }
    val inverter = devices.firstOrNull { it.deviceKind() == DeviceKind.Inverter }
    RoadPanelCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("Énergie", style = MaterialTheme.typography.titleMedium)
                    Text(summarySubtitle(state), style = MaterialTheme.typography.bodyMedium, color = RoadPanelMuted)
                }
                ConnectionStatusChip(connected = state.health?.mqttConnected ?: (state.metrics != null))
            }
            if (state.errorMessage != null && state.metrics == null) {
                InlineAction("VECTRON indisponible", state.errorMessage, RoadPanelError, onRefresh)
            } else {
                MetricGrid(listOf(
                    Metric("Batterie", solar?.batteryVoltage?.format(1) ?: inverter?.batteryVoltage?.format(1) ?: "--", "V"),
                    Metric("Solaire", solar?.solarPower?.format(0) ?: "--", "W"),
                    Metric("Onduleur", inverter?.acApparentPower?.format(0) ?: "--", "VA"),
                    Metric("Appareils", devices.size.toString()),
                ))
            }
        }
    }
}

@Composable
private fun InlineAction(text: String, detail: String, color: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(Modifier.fillMaxWidth().clickable(interaction, null, onClick = onClick), shape = RoundedCornerShape(18.dp), color = color.copy(alpha = .10f)) {
        Column(Modifier.padding(14.dp)) {
            Text(text, color = color, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.labelMedium, color = RoadPanelMuted)
        }
    }
}

@Composable
private fun LoadingEnergyCard() {
    RoadPanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(Modifier.size(42.dp), color = RoadPanelAccent, strokeWidth = 4.dp)
            Text("Lecture des appareils VECTRON…", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun StateCard(title: String, detail: String, color: Color, onRetry: () -> Unit) {
    RoadPanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBubble(RoadPanelIconKind.Connection, color, 46)
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = RoadPanelMuted)
            InlineAction("Actualiser", "Relancer la lecture", color, onRetry)
        }
    }
}

@Composable
private fun IconBubble(icon: RoadPanelIconKind, accent: Color, size: Int) {
    Surface(Modifier.size(size.dp), shape = CircleShape, color = accent.copy(alpha = .12f)) {
        RoadPanelIcon(icon, Modifier.padding((size * .24f).dp), accent)
    }
}

private enum class DeviceKind(val title: String, val icon: RoadPanelIconKind, val accent: Color, val order: Int) {
    Solar("Chargeur solaire", RoadPanelIconKind.Solar, RoadPanelSolar, 0),
    BatteryProtect("Protection batterie", RoadPanelIconKind.Battery, RoadPanelAccent, 1),
    Inverter("Onduleur AC", RoadPanelIconKind.Power, RoadPanelSky, 2),
    Other("Appareil Victron", RoadPanelIconKind.Connection, RoadPanelMuted, 3),
}

private fun VictronDeviceDto.deviceKind(): DeviceKind {
    val signature = listOfNotNull(name, modelName).joinToString(" ").lowercase(Locale.US)
    return when {
        solarPower != null || "smartsolar" in signature || "mppt" in signature -> DeviceKind.Solar
        inputVoltage != null || "batteryprotect" in signature || "batteryprotec" in signature -> DeviceKind.BatteryProtect
        acApparentPower != null || "inverter" in signature || "ve_direct" in signature -> DeviceKind.Inverter
        else -> DeviceKind.Other
    }
}

private fun VictronDeviceDto.inverterCapacityVa(): Double? = modelName
    ?.let { Regex("(\\d+)\\s*VA", RegexOption.IGNORE_CASE).find(it) }
    ?.groupValues?.getOrNull(1)?.toDoubleOrNull()

private fun Double.metric(label: String, unit: String, decimals: Int) = Metric(label, format(decimals), unit)
private fun Double.format(decimals: Int): String = "%.${decimals}f".format(Locale.US, this)

private fun String.toReadableLabel(): String = split("_", "-", " ")
    .filter(String::isNotBlank)
    .joinToString(" ") { it.lowercase(Locale.US).replaceFirstChar { char -> char.titlecase(Locale.getDefault()) } }

private fun String.toReadableStatus(): String = when (lowercase(Locale.US)) {
    "bulk" -> "Charge principale"; "absorption" -> "Absorption"; "float" -> "Maintien"
    "inverting" -> "En conversion"; "active" -> "Actif"; "on" -> "Activée"; "off" -> "Désactivée"
    "no_error" -> "Aucune erreur"; "no_alarm" -> "Aucune alarme"; "no_reason" -> "Aucune"
    else -> toReadableLabel()
}

private fun String?.isHealthyStatus(): Boolean = this == null || lowercase(Locale.US) in setOf("no_error", "no_alarm", "no_reason", "ok")
private fun rssiColor(rssi: Int): Color = when { rssi >= -60 -> RoadPanelAccent; rssi >= -75 -> RoadPanelWarning; else -> RoadPanelError }

private fun energyStatusText(state: EnergyUiState): String = when {
    state.errorMessage != null && state.metrics == null -> "Hors ligne"; state.waitingForMqttData -> "En attente"
    state.isLoading -> "Synchronisation"; state.metrics != null -> "En direct"; else -> "Inactif"
}
private fun energyStatusColor(state: EnergyUiState): Color = when {
    state.errorMessage != null && state.metrics == null -> RoadPanelError; state.waitingForMqttData -> RoadPanelWarning
    state.metrics != null -> RoadPanelAccent; else -> RoadPanelMuted
}
private fun summarySubtitle(state: EnergyUiState): String = when {
    state.waitingForMqttData -> "En attente de télémétrie"
    state.errorMessage != null && state.metrics == null -> "API VECTRON hors ligne"
    state.metrics != null -> "${state.metrics.size} appareil${if (state.metrics.size > 1) "s" else ""} en direct"
    state.isLoading -> "Synchronisation VECTRON"; else -> "Aucune donnée"
}

@Preview(showBackground = true)
@Composable
private fun EnergyContentPreview() {
    RoadPanelTheme {
        EnergyContent(EnergyUiState(
            health = VictronHealthDto("ok", true, "2026-09-07T16:37:00.868703+00:00"),
            metrics = sampleDevices(), lastUpdated = "2026-09-07T16:37:00.868703+00:00",
        ), {})
    }
}

private fun sampleDevices() = mapOf(
    "smartsolar_pyleas" to VictronDeviceDto(timestamp = "2026-09-07T16:37:00.868703+00:00", name = "smartsolar_pyleas", address = "E1:EA:0C:89:CC:C5", rssi = -56, modelName = "SmartSolar Charger MPPT 100/30", batteryChargingCurrent = .8, batteryVoltage = 12.82, chargeState = "bulk", chargerError = "no_error", solarPower = 10.0, yieldToday = 480.0),
    "batteryprotec_pyleas" to VictronDeviceDto(timestamp = "2026-09-07T16:37:00.877750+00:00", name = "batteryprotec_pyleas", address = "DF:B9:BA:2B:9A:B0", rssi = -58, modelName = "Smart BatteryProtect 12/24V-65A", alarmReason = "no_alarm", deviceState = "active", errorCode = "no_error", inputVoltage = 12.79, offReason = "no_reason", outputState = "on", outputVoltage = 12.79, warningReason = "no_alarm"),
    "ve_direct_pyleas" to VictronDeviceDto(timestamp = "2026-09-07T16:37:00.875711+00:00", name = "ve_direct_pyleas", address = "C8:29:EC:61:6D:5B", rssi = -46, modelName = "Phoenix Inverter 12V 500VA 230V", acApparentPower = 230.0, acCurrent = 1.0, acVoltage = 230.02, batteryVoltage = 12.65, deviceState = "inverting"),
)
