package ch.bus.roadpanel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.bus.roadpanel.ui.theme.RoadPanelAccent
import ch.bus.roadpanel.ui.theme.RoadPanelAccentSoft
import ch.bus.roadpanel.ui.theme.RoadPanelError
import ch.bus.roadpanel.ui.theme.RoadPanelLine
import ch.bus.roadpanel.ui.theme.RoadPanelMuted
import ch.bus.roadpanel.ui.theme.RoadPanelSurface
import ch.bus.roadpanel.ui.theme.RoadPanelTheme

@Composable
fun RoadPanelCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = RoadPanelSurface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, RoadPanelLine.copy(alpha = 0.72f)),
        content = content,
    )
}

@Composable
fun FloatingMapCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = RoadPanelSurface.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.76f)),
        content = content,
    )
}

@Composable
fun TelemetryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    status: String? = null,
    icon: RoadPanelIconKind? = null,
    accent: Color = RoadPanelAccent,
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
                    style = MaterialTheme.typography.labelLarge,
                    color = RoadPanelMuted,
                )
                icon?.let {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.12f),
                        contentColor = accent,
                    ) {
                        RoadPanelIcon(
                            kind = it,
                            modifier = Modifier
                                .padding(9.dp)
                                .size(20.dp),
                            color = accent,
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                unit?.let {
                    Text(
                        modifier = Modifier.padding(start = 5.dp, bottom = 5.dp),
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = RoadPanelMuted,
                    )
                }
            }

            status?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoadPanelMuted,
                )
            }
        }
    }
}

@Composable
fun DashboardSection(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            trailing?.invoke(this)
        }
        content()
    }
}

@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = RoadPanelAccent,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Spacer(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
fun MetricValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    large: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = RoadPanelMuted,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = if (large) MaterialTheme.typography.displayLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            unit?.let {
                Text(
                    modifier = Modifier.padding(start = 5.dp, bottom = if (large) 9.dp else 4.dp),
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = RoadPanelMuted,
                )
            }
        }
    }
}

@Composable
fun ConnectionStatusChip(
    connected: Boolean?,
    modifier: Modifier = Modifier,
) {
    val text = when (connected) {
        true -> "MQTT connected"
        false -> "MQTT offline"
        null -> "MQTT unknown"
    }
    val color = when (connected) {
        true -> RoadPanelAccent
        false -> RoadPanelError
        null -> RoadPanelMuted
    }
    StatusPill(
        modifier = modifier,
        text = text,
        color = color,
    )
}

@Preview(showBackground = true)
@Composable
private fun TelemetryCardPreview() {
    RoadPanelTheme {
        TelemetryCard(
            title = "Altitude",
            value = "1,248",
            unit = "m",
            status = "Stable sur le dernier relevé",
            icon = RoadPanelIconKind.Altitude,
            accent = RoadPanelAccentSoft,
        )
    }
}
