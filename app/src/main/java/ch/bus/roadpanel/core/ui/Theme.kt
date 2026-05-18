package ch.bus.roadpanel.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RoadPanelDark = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    secondary = Color(0xFF26A69A),
    background = Color(0xFF0B1020),
    surface = Color(0xFF141B2D),
)

@Composable
fun RoadPanelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RoadPanelDark,
        typography = MaterialTheme.typography,
        content = content,
    )
}
