package ch.bus.roadpanel.feature.gps.ui

import android.annotation.SuppressLint
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.bus.roadpanel.core.network.NetworkModule
import ch.bus.roadpanel.feature.gps.domain.GpsRepository
import ch.bus.roadpanel.ui.components.FloatingMapCard
import ch.bus.roadpanel.ui.components.RoadPanelIcon
import ch.bus.roadpanel.ui.components.RoadPanelIconKind
import ch.bus.roadpanel.ui.components.StatusPill
import ch.bus.roadpanel.ui.theme.RoadPanelAccent
import ch.bus.roadpanel.ui.theme.RoadPanelCanvas
import ch.bus.roadpanel.ui.theme.RoadPanelError
import ch.bus.roadpanel.ui.theme.RoadPanelMuted
import ch.bus.roadpanel.ui.theme.RoadPanelSky
import ch.bus.roadpanel.ui.theme.RoadPanelSurface
import ch.bus.roadpanel.ui.theme.RoadPanelTheme
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GpsScreen(modifier: Modifier = Modifier) {
    val viewModel: GpsViewModel = viewModel(factory = GpsViewModel.factory(GpsRepository(NetworkModule.gpsApi)))
    val state by viewModel.uiState.collectAsState()
    var orthophotoEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Configuration.getInstance().userAgentValue = context.packageName
    val orthophotoTileSource = remember {
        XYTileSource(
            "EsriWorldImagery",
            0,
            19,
            256,
            ".jpg",
            arrayOf("https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RoadPanelCanvas),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                MapView(it).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    minZoomLevel = 3.0
                    maxZoomLevel = 20.0
                    controller.setZoom(17.0)
                }
            },
            update = { map ->
                map.setTileSource(if (orthophotoEnabled) orthophotoTileSource else TileSourceFactory.MAPNIK)
                state.data?.let {
                    val point = GeoPoint(it.mapLatitude, it.mapLongitude)
                    map.controller.animateTo(point)
                    map.overlays.removeAll { overlay -> overlay is VehiclePositionOverlay }
                    map.overlays.add(VehiclePositionOverlay(point))
                    map.invalidate()
                }
            },
        )

        StatusPill(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 24.dp),
            text = if (orthophotoEnabled) "Satellite" else "Carte",
            color = RoadPanelAccent,
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 22.dp, end = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MapControlButton(
                icon = RoadPanelIconKind.Refresh,
                onClick = viewModel::refresh,
            )
            MapControlButton(
                icon = RoadPanelIconKind.Layers,
                selected = orthophotoEnabled,
                onClick = { orthophotoEnabled = !orthophotoEnabled },
            )
            MapControlButton(
                icon = RoadPanelIconKind.Locate,
                onClick = viewModel::refresh,
            )
        }

        GpsTelemetrySheet(
            state = state,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 136.dp),
        )
    }
}

@Composable
private fun MapControlButton(
    icon: RoadPanelIconKind,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .size(52.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = CircleShape,
        color = if (selected) RoadPanelAccent else RoadPanelSurface.copy(alpha = 0.94f),
        contentColor = if (selected) RoadPanelSurface else MaterialTheme.colorScheme.onSurface,
        shadowElevation = 12.dp,
        tonalElevation = 0.dp,
    ) {
        RoadPanelIcon(
            kind = icon,
            modifier = Modifier
                .padding(14.dp)
                .size(24.dp),
            color = if (selected) RoadPanelSurface else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun GpsTelemetrySheet(
    state: GpsUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingMapCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "GPS en direct",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = state.data?.time ?: "En attente du flux véhicule",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoadPanelMuted,
                    )
                }
                StatusPill(
                    text = gpsStatus(state),
                    color = if (state.error == null) RoadPanelAccent else RoadPanelError,
                )
            }

            state.error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoadPanelError,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GpsStat(
                    modifier = Modifier.weight(1.2f),
                    label = "Vitesse",
                    value = state.data?.speed?.format(1) ?: "--",
                    unit = "km/h",
                )
                GpsStat(
                    modifier = Modifier.weight(1f),
                    label = "Altitude",
                    value = state.data?.altitude?.format(0) ?: "--",
                    unit = "m",
                )
                GpsStat(
                    modifier = Modifier.weight(1f),
                    label = "Cap",
                    value = state.data?.track?.format(0) ?: "--",
                    unit = "deg",
                )
            }
        }
    }
}

@Composable
private fun GpsStat(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = RoadPanelMuted,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                text = unit,
                style = MaterialTheme.typography.labelMedium,
                color = RoadPanelMuted,
            )
        }
    }
}

private fun gpsStatus(state: GpsUiState): String = when {
    state.error != null -> "Hors ligne"
    state.isLoading -> "Synchronisation"
    state.data != null -> "Verrouillé"
    else -> "Veille"
}

private fun Double.format(decimals: Int): String = "%.${decimals}f".format(Locale.US, this)

private class VehiclePositionOverlay(private val point: GeoPoint) : Overlay() {
    private val outerPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
        setShadowLayer(8f, 0f, 3f, android.graphics.Color.argb(70, 0, 0, 0))
    }
    private val innerPaint = Paint().apply {
        color = android.graphics.Color.rgb(16, 18, 20)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val ringPaint = Paint().apply {
        color = android.graphics.Color.argb(54, 30, 107, 92)
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    override fun draw(canvas: android.graphics.Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val screenPoint = android.graphics.Point()
        mapView.projection.toPixels(point, screenPoint)
        canvas.drawCircle(screenPoint.x.toFloat(), screenPoint.y.toFloat(), 24f, ringPaint)
        canvas.drawCircle(screenPoint.x.toFloat(), screenPoint.y.toFloat(), 14f, outerPaint)
        canvas.drawCircle(screenPoint.x.toFloat(), screenPoint.y.toFloat(), 7f, innerPaint)
    }
}

@Preview(showBackground = true)
@Composable
private fun GpsTelemetrySheetPreview() {
    RoadPanelTheme {
        GpsTelemetrySheet(
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
            onRefresh = {},
        )
    }
}
