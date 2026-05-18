package ch.bus.roadpanel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ch.bus.roadpanel.core.ui.RoadPanelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoadPanelTheme {
                RoadPanelApp()
            }
        }
    }
}
