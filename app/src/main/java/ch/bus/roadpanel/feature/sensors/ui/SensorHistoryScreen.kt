package ch.bus.roadpanel.feature.sensors.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.bus.roadpanel.core.network.NetworkModule
import ch.bus.roadpanel.feature.sensors.data.SensorDto
import ch.bus.roadpanel.feature.sensors.domain.SensorsRepository
import ch.bus.roadpanel.ui.components.RoadPanelCard
import ch.bus.roadpanel.ui.theme.RoadPanelAccent
import ch.bus.roadpanel.ui.theme.RoadPanelCanvas
import ch.bus.roadpanel.ui.theme.RoadPanelError
import ch.bus.roadpanel.ui.theme.RoadPanelLine
import ch.bus.roadpanel.ui.theme.RoadPanelMuted
import ch.bus.roadpanel.ui.theme.RoadPanelSky
import ch.bus.roadpanel.ui.theme.RoadPanelSurface
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private val historyDateFormatter = DateTimeFormatter.ofPattern("dd-MM\nHH-mm")
private val historySelectionDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm:ss")

@Composable
fun SensorHistoryScreen(
    sensorId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LockLandscapeOrientation()
    BackHandler(onBack = onBack)

    val historyViewModel: SensorHistoryViewModel = viewModel(
        key = "history-$sensorId",
        factory = SensorHistoryViewModel.factory(
            sensorId = sensorId,
            repository = SensorsRepository(NetworkModule.sensorsApi),
        ),
    )
    val state by historyViewModel.uiState.collectAsState()
    val readings = state.history?.readings.orEmpty()
    val sensorName = readings.firstOrNull()?.name ?: sensorId.replace('_', ' ')

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RoadPanelCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HistoryHeader(
            sensorName = sensorName,
            readingCount = state.history?.readingCount,
            onBack = onBack,
            onRefresh = historyViewModel::refresh,
        )

        RoadPanelCard(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                when {
                    state.isLoading && state.history == null -> CircularProgressIndicator(color = RoadPanelAccent)
                    state.errorMessage != null && state.history == null -> ErrorContent(historyViewModel::refresh)
                    readings.isEmpty() -> Text(
                        "Aucune mesure disponible pour ce capteur.",
                        color = RoadPanelMuted,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    else -> SensorHistoryChart(readings = readings, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun LockLandscapeOrientation() {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity) {
        val previousOrientation = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { activity.requestedOrientation = previousOrientation }
    }
}

@Composable
private fun HistoryHeader(
    sensorName: String,
    readingCount: Int?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(44.dp).clickable(onClick = onBack),
                shape = CircleShape,
                color = RoadPanelSurface,
                shadowElevation = 5.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("‹", color = RoadPanelAccent, style = MaterialTheme.typography.headlineMedium)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = sensorName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = readingCount?.let { "$it mesures dans l’historique" } ?: "Historique du capteur",
                    color = RoadPanelMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Text(
            text = "Actualiser",
            modifier = Modifier.clickable(onClick = onRefresh).padding(10.dp),
            color = RoadPanelAccent,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Historique indisponible", color = RoadPanelError, style = MaterialTheme.typography.titleMedium)
        Text(
            "Réessayer",
            modifier = Modifier.clickable(onClick = onRetry).padding(8.dp),
            color = RoadPanelAccent,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private data class HistoryPoint(
    val timeMillis: Long,
    val temperature: Double?,
    val humidity: Double?,
)

private enum class HistorySeries { TEMPERATURE, HUMIDITY }

private data class HistorySelection(
    val pointIndex: Int,
    val series: HistorySeries,
)

@Composable
private fun SensorHistoryChart(readings: List<SensorDto>, modifier: Modifier = Modifier) {
    val points = readings.mapIndexedNotNull { index, reading ->
        val time = parseHistoryTime(reading.receivedAt ?: reading.timestamp)
            ?: (index.toLong() * 60_000L)
        HistoryPoint(time, reading.temperature, reading.humidity)
    }.sortedBy(HistoryPoint::timeMillis)

    val temperatures = points.mapNotNull(HistoryPoint::temperature)
    val humidities = points.mapNotNull(HistoryPoint::humidity)
    val temperatureRange = paddedRange(temperatures, defaultMin = 0.0, defaultMax = 40.0)
    val humidityRange = paddedRange(humidities, defaultMin = 0.0, defaultMax = 100.0)
    val temperatureColor = RoadPanelAccent
    val humidityColor = RoadPanelSky
    var selection by remember(points) { mutableStateOf<HistorySelection?>(null) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ChartLegend("Température  °C", temperatureColor)
            ChartLegend("Humidité  %", humidityColor)
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points, temperatureRange, humidityRange) {
                    detectTapGestures { position ->
                        selection = closestHistorySelection(
                            touch = position,
                            canvasWidth = size.width.toFloat(),
                            canvasHeight = size.height.toFloat(),
                            points = points,
                            temperatureRange = temperatureRange,
                            humidityRange = humidityRange,
                            left = 62.dp.toPx(),
                            rightPadding = 64.dp.toPx(),
                            top = 12.dp.toPx(),
                            bottomPadding = 44.dp.toPx(),
                        )
                    }
                }
                .pointerInput(points, temperatureRange, humidityRange) {
                    fun select(position: Offset) {
                        selection = closestHistorySelection(
                            touch = position,
                            canvasWidth = size.width.toFloat(),
                            canvasHeight = size.height.toFloat(),
                            points = points,
                            temperatureRange = temperatureRange,
                            humidityRange = humidityRange,
                            left = 62.dp.toPx(),
                            rightPadding = 64.dp.toPx(),
                            top = 12.dp.toPx(),
                            bottomPadding = 44.dp.toPx(),
                        )
                    }
                    detectDragGestures(
                        onDragStart = ::select,
                        onDrag = { change, _ ->
                            change.consume()
                            select(change.position)
                        },
                    )
                },
        ) {
            val left = 62.dp.toPx()
            val right = size.width - 64.dp.toPx()
            val top = 12.dp.toPx()
            val bottom = size.height - 44.dp.toPx()
            if (right <= left || bottom <= top) return@Canvas

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 11.dp.toPx()
                color = android.graphics.Color.rgb(111, 118, 125)
                textAlign = Paint.Align.RIGHT
            }
            val xLabelPaint = Paint(labelPaint).apply { textAlign = Paint.Align.CENTER }
            val rightLabelPaint = Paint(labelPaint).apply { textAlign = Paint.Align.LEFT }

            repeat(5) { tick ->
                val fraction = tick / 4f
                val y = bottom - fraction * (bottom - top)
                drawLine(RoadPanelLine, Offset(left, y), Offset(right, y), 1.dp.toPx())
                val temperatureValue = temperatureRange.first + fraction * (temperatureRange.second - temperatureRange.first)
                val humidityValue = humidityRange.first + fraction * (humidityRange.second - humidityRange.first)
                drawContext.canvas.nativeCanvas.drawText(
                    String.format(Locale.getDefault(), "%.1f°", temperatureValue),
                    left - 8.dp.toPx(), y + 4.dp.toPx(), labelPaint,
                )
                drawContext.canvas.nativeCanvas.drawText(
                    String.format(Locale.getDefault(), "%.0f%%", humidityValue),
                    right + 8.dp.toPx(), y + 4.dp.toPx(), rightLabelPaint,
                )
            }

            val firstTime = points.first().timeMillis
            val lastTime = points.last().timeMillis
            val timeSpan = max(1L, lastTime - firstTime)
            fun x(time: Long): Float = left + ((time - firstTime).toFloat() / timeSpan) * (right - left)
            fun y(value: Double, range: Pair<Double, Double>): Float =
                bottom - ((value - range.first) / (range.second - range.first)).toFloat() * (bottom - top)

            val labelIndices = (0 until min(5, points.size)).map { position ->
                if (points.size == 1) 0 else position * (points.lastIndex) / (min(5, points.size) - 1)
            }.distinct()
            labelIndices.forEach { index ->
                val point = points[index]
                val lines = formatHistoryTime(point.timeMillis).split('\n')
                val labelX = x(point.timeMillis)
                lines.forEachIndexed { lineIndex, line ->
                    drawContext.canvas.nativeCanvas.drawText(
                        line,
                        labelX,
                        bottom + (16 + lineIndex * 13).dp.toPx(),
                        xLabelPaint,
                    )
                }
            }

            drawSeries(points, temperatureColor, { it.temperature }, { y(it, temperatureRange) }, ::x)
            drawSeries(points, humidityColor, { it.humidity }, { y(it, humidityRange) }, ::x)

            selection?.let { selected ->
                val point = points.getOrNull(selected.pointIndex) ?: return@let
                val value = when (selected.series) {
                    HistorySeries.TEMPERATURE -> point.temperature
                    HistorySeries.HUMIDITY -> point.humidity
                } ?: return@let
                val color = when (selected.series) {
                    HistorySeries.TEMPERATURE -> temperatureColor
                    HistorySeries.HUMIDITY -> humidityColor
                }
                val selectedX = x(point.timeMillis)
                val selectedY = when (selected.series) {
                    HistorySeries.TEMPERATURE -> y(value, temperatureRange)
                    HistorySeries.HUMIDITY -> y(value, humidityRange)
                }

                drawLine(
                    color = color.copy(alpha = 0.38f),
                    start = Offset(selectedX, top),
                    end = Offset(selectedX, bottom),
                    strokeWidth = 2.dp.toPx(),
                )
                drawCircle(Color.White, radius = 10.dp.toPx(), center = Offset(selectedX, selectedY))
                drawCircle(color.copy(alpha = 0.25f), radius = 14.dp.toPx(), center = Offset(selectedX, selectedY))
                drawCircle(color, radius = 6.dp.toPx(), center = Offset(selectedX, selectedY))
                drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(selectedX, selectedY))

                drawHistoryPopover(
                    anchor = Offset(selectedX, selectedY),
                    plotLeft = left,
                    plotRight = right,
                    plotTop = top,
                    plotBottom = bottom,
                    color = color,
                    value = value,
                    series = selected.series,
                    timeMillis = point.timeMillis,
                )
            }
        }
    }
}

@Composable
private fun ChartLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = color) {}
        Spacer(Modifier.width(7.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSeries(
    points: List<HistoryPoint>,
    color: Color,
    value: (HistoryPoint) -> Double?,
    y: (Double) -> Float,
    x: (Long) -> Float,
) {
    var path = Path()
    var hasPoint = false
    points.forEach { point ->
        val current = value(point)
        if (current == null) {
            if (hasPoint) drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            path = Path()
            hasPoint = false
        } else {
            val position = Offset(x(point.timeMillis), y(current))
            if (hasPoint) path.lineTo(position.x, position.y) else path.moveTo(position.x, position.y)
            hasPoint = true
            drawCircle(color, radius = 3.dp.toPx(), center = position)
        }
    }
    if (hasPoint) drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
}

private fun closestHistorySelection(
    touch: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    points: List<HistoryPoint>,
    temperatureRange: Pair<Double, Double>,
    humidityRange: Pair<Double, Double>,
    left: Float,
    rightPadding: Float,
    top: Float,
    bottomPadding: Float,
): HistorySelection? {
    if (points.isEmpty()) return null
    val right = canvasWidth - rightPadding
    val bottom = canvasHeight - bottomPadding
    if (right <= left || bottom <= top) return null
    val firstTime = points.first().timeMillis
    val timeSpan = max(1L, points.last().timeMillis - firstTime)

    fun x(time: Long): Float = left + ((time - firstTime).toFloat() / timeSpan) * (right - left)
    fun y(value: Double, range: Pair<Double, Double>): Float =
        bottom - ((value - range.first) / (range.second - range.first)).toFloat() * (bottom - top)

    var closest: HistorySelection? = null
    var closestDistance = Float.MAX_VALUE
    points.forEachIndexed { index, point ->
        fun consider(series: HistorySeries, value: Double?, range: Pair<Double, Double>) {
            if (value == null) return
            val dx = touch.x - x(point.timeMillis)
            val dy = touch.y - y(value, range)
            val distance = dx * dx + dy * dy
            if (distance < closestDistance) {
                closestDistance = distance
                closest = HistorySelection(index, series)
            }
        }
        consider(HistorySeries.TEMPERATURE, point.temperature, temperatureRange)
        consider(HistorySeries.HUMIDITY, point.humidity, humidityRange)
    }
    return closest
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHistoryPopover(
    anchor: Offset,
    plotLeft: Float,
    plotRight: Float,
    plotTop: Float,
    plotBottom: Float,
    color: Color,
    value: Double,
    series: HistorySeries,
    timeMillis: Long,
) {
    val popupWidth = min(206.dp.toPx(), plotRight - plotLeft)
    val popupHeight = 64.dp.toPx()
    val gap = 18.dp.toPx()
    val popupLeft = (anchor.x - popupWidth / 2f).coerceIn(plotLeft, plotRight - popupWidth)
    val preferredTop = anchor.y - gap - popupHeight
    val popupTop = if (preferredTop >= plotTop) {
        preferredTop
    } else {
        (anchor.y + gap).coerceAtMost(plotBottom - popupHeight)
    }

    drawRoundRect(
        color = RoadPanelSurface.copy(alpha = 0.97f),
        topLeft = Offset(popupLeft, popupTop),
        size = androidx.compose.ui.geometry.Size(popupWidth, popupHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(popupLeft, popupTop),
        size = androidx.compose.ui.geometry.Size(5.dp.toPx(), popupHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
    )

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 15.dp.toPx()
        this.color = color.toArgb()
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12.dp.toPx()
        this.color = RoadPanelMuted.toArgb()
    }
    val valueText = when (series) {
        HistorySeries.TEMPERATURE -> String.format(Locale.getDefault(), "Température  %.1f °C", value)
        HistorySeries.HUMIDITY -> String.format(Locale.getDefault(), "Humidité  %.1f %%", value)
    }
    val dateText = Instant.ofEpochMilli(timeMillis)
        .atZone(ZoneId.systemDefault())
        .format(historySelectionDateFormatter)
    drawContext.canvas.nativeCanvas.drawText(
        valueText,
        popupLeft + 16.dp.toPx(),
        popupTop + 25.dp.toPx(),
        titlePaint,
    )
    drawContext.canvas.nativeCanvas.drawText(
        dateText,
        popupLeft + 16.dp.toPx(),
        popupTop + 48.dp.toPx(),
        datePaint,
    )
}

private fun paddedRange(values: List<Double>, defaultMin: Double, defaultMax: Double): Pair<Double, Double> {
    if (values.isEmpty()) return defaultMin to defaultMax
    val minimum = values.minOrNull() ?: defaultMin
    val maximum = values.maxOrNull() ?: defaultMax
    val padding = max((maximum - minimum) * 0.12, 1.0)
    return (minimum - padding) to (maximum + padding)
}

private fun parseHistoryTime(value: String?): Long? {
    if (value == null) return null
    return try {
        OffsetDateTime.parse(value).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        try { Instant.parse(value).toEpochMilli() } catch (_: DateTimeParseException) { null }
    }
}

private fun formatHistoryTime(timeMillis: Long): String =
    Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()).format(historyDateFormatter)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
