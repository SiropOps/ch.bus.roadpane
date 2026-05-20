package ch.bus.roadpanel.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val RoadPanelLightColorScheme = lightColorScheme(
    primary = RoadPanelAccent,
    onPrimary = RoadPanelSurface,
    primaryContainer = RoadPanelAccentSoft,
    onPrimaryContainer = RoadPanelInk,
    secondary = RoadPanelSky,
    onSecondary = RoadPanelSurface,
    background = RoadPanelCanvas,
    onBackground = RoadPanelInk,
    surface = RoadPanelSurface,
    onSurface = RoadPanelInk,
    surfaceVariant = RoadPanelSurfaceSoft,
    onSurfaceVariant = RoadPanelMuted,
    outline = RoadPanelLine,
    error = RoadPanelError,
)

@Composable
fun RoadPanelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> RoadPanelLightColorScheme
    }

    CompositionLocalProvider(LocalRoadPanelSpacing provides RoadPanelSpacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RoadPanelTypography,
            content = content,
        )
    }
}

object RoadPanelTokens {
    val spacing: RoadPanelSpacing
        @Composable
        get() = LocalRoadPanelSpacing.current
}
