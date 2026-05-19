package ch.bus.roadpanel.feature.gps.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.bus.roadpanel.core.network.NetworkModule
import ch.bus.roadpanel.R
import ch.bus.roadpanel.feature.gps.domain.GpsRepository
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GpsScreen(modifier: Modifier = Modifier) {
    val viewModel: GpsViewModel = viewModel(factory = GpsViewModel.factory(GpsRepository(NetworkModule.gpsApi)))
    val state by viewModel.uiState.collectAsState()
    var orthophotoEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Configuration.getInstance().userAgentValue = context.packageName
    val orthophotoTileSource = XYTileSource(
        "EsriWorldImagery",
        0,
        19,
        256,
        ".jpg",
        arrayOf("https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
    )

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                MapView(it).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(17.0)
                }
            },
            update = { map ->
                map.setTileSource(if (orthophotoEnabled) orthophotoTileSource else TileSourceFactory.MAPNIK)
                state.data?.let {
                    val point = GeoPoint(it.mapLatitude, it.mapLongitude)
                    map.controller.animateTo(point)
                    map.overlays.removeAll { overlay -> overlay is Marker }
                    map.overlays.add(
                        Marker(map).apply {
                            position = point
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_van_marker)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                    )
                    map.invalidate()
                }
            }
        )

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("GPS", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = viewModel::refresh) { Text("Refresh") }
                }

                if (state.isLoading) Text("Connexion: loading...")
                state.error?.let { Text("Erreur: $it", color = Color.Red) }
                state.data?.let {
                    Text("Heure GPS: ${it.time}")
                }
            }
        }
    }
}
