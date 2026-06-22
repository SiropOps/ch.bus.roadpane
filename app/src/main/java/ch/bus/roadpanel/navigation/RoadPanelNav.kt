package ch.bus.roadpanel.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.bus.roadpanel.feature.control.ui.ControlScreen
import ch.bus.roadpanel.feature.dashboard.ui.DashboardScreen
import ch.bus.roadpanel.feature.energy.ui.EnergyScreen
import ch.bus.roadpanel.feature.gps.ui.GpsScreen
import ch.bus.roadpanel.feature.sensors.ui.SensorsScreen
import ch.bus.roadpanel.ui.components.RoadPanelCard
import ch.bus.roadpanel.ui.components.RoadPanelIcon
import ch.bus.roadpanel.ui.components.RoadPanelIconKind
import ch.bus.roadpanel.ui.components.roadPanelBottomBarContentPadding
import ch.bus.roadpanel.ui.theme.RoadPanelAccent
import ch.bus.roadpanel.ui.theme.RoadPanelCanvas
import ch.bus.roadpanel.ui.theme.RoadPanelMuted
import ch.bus.roadpanel.ui.theme.RoadPanelSurface

sealed class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: RoadPanelIconKind,
) {
    data object Home : TopLevelDestination("home", "Accueil", RoadPanelIconKind.Dashboard)
    data object Gps : TopLevelDestination("gps", "Carte", RoadPanelIconKind.Map)
    data object Power : TopLevelDestination("power", "Énergie", RoadPanelIconKind.Power)
    data object Sensors : TopLevelDestination("sensors", "Capteurs", RoadPanelIconKind.Sensors)
    data object Settings : TopLevelDestination("settings", "Commandes", RoadPanelIconKind.Settings)
}

@Composable
fun RoadPanelApp() {
    val navController = rememberNavController()
    val destinations = listOf(
        TopLevelDestination.Home,
        TopLevelDestination.Gps,
        TopLevelDestination.Power,
        TopLevelDestination.Sensors,
        TopLevelDestination.Settings,
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RoadPanelCanvas),
    ) {
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Home.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(TopLevelDestination.Home.route) {
                StatusBarInsetScreen { DashboardScreen() }
            }
            composable(TopLevelDestination.Gps.route) { GpsScreen() }
            composable(TopLevelDestination.Power.route) {
                StatusBarInsetScreen { EnergyScreen() }
            }
            composable(TopLevelDestination.Sensors.route) {
                StatusBarInsetScreen { SensorsScreen() }
            }
            composable(TopLevelDestination.Settings.route) {
                StatusBarInsetScreen { ControlScreen() }
            }
        }

        RoadPanelBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            destinations = destinations,
            currentRoute = currentDestination?.hierarchy?.firstOrNull()?.route,
            onDestinationSelected = { destination ->
                navController.navigate(destination.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }
}

@Composable
private fun StatusBarInsetScreen(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        content()
    }
}

@Composable
private fun RoadPanelBottomBar(
    destinations: List<TopLevelDestination>,
    currentRoute: String?,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        color = RoadPanelSurface.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 16.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                val selected = currentRoute == destination.route
                BottomBarItem(
                    destination = destination,
                    selected = selected,
                    onClick = { onDestinationSelected(destination) },
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val itemWidth by animateDpAsState(
        targetValue = if (selected) 68.dp else 48.dp,
        animationSpec = tween(220),
        label = "bottomItemWidth",
    )
    val background by animateColorAsState(
        targetValue = if (selected) RoadPanelAccent.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(220),
        label = "bottomItemBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) RoadPanelAccent else RoadPanelMuted,
        animationSpec = tween(220),
        label = "bottomItemContent",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .width(itemWidth)
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RoadPanelIcon(
            kind = destination.icon,
            modifier = Modifier.size(24.dp),
            color = contentColor,
        )
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun PlaceholderScreen(label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RoadPanelCanvas)
            .padding(
                start = 22.dp,
                top = 16.dp,
                end = 22.dp,
                bottom = roadPanelBottomBarContentPadding(),
            ),
        contentAlignment = Alignment.Center,
    ) {
        RoadPanelCard {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Réservé à la télémétrie du véhicule à venir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoadPanelMuted,
                )
            }
        }
    }
}
