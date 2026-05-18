package ch.bus.roadpanel.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.bus.roadpanel.feature.gps.ui.GpsScreen

sealed class TopLevelDestination(val route: String, val label: String, val icon: String) {
    data object Gps : TopLevelDestination("gps", "GPS", "📍")
    data object Power : TopLevelDestination("power", "Power", "🔋")
    data object Sensors : TopLevelDestination("sensors", "Sensors", "🧪")
    data object Settings : TopLevelDestination("settings", "Settings", "⚙️")
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RoadPanelApp() {
    val navController = rememberNavController()
    val destinations = listOf(TopLevelDestination.Gps, TopLevelDestination.Power, TopLevelDestination.Sensors, TopLevelDestination.Settings)

    Scaffold(
        topBar = { TopAppBar(title = { Text("RoadPanel") }) },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = { navController.navigate(destination.route) },
                        icon = { Text(destination.icon) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = TopLevelDestination.Gps.route, modifier = Modifier.padding(padding)) {
            composable(TopLevelDestination.Gps.route) { GpsScreen() }
            composable(TopLevelDestination.Power.route) { PlaceholderScreen("Power") }
            composable(TopLevelDestination.Sensors.route) { PlaceholderScreen("Sensors") }
            composable(TopLevelDestination.Settings.route) { PlaceholderScreen("Settings") }
        }
    }
}

@Composable
private fun PlaceholderScreen(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("$label - bientôt disponible") }
}
