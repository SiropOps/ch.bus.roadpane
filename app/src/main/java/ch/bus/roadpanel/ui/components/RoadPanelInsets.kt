package ch.bus.roadpanel.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val BottomBarContentClearance = 126.dp

@Composable
fun roadPanelBottomBarContentPadding(extra: Dp = 0.dp): Dp {
    val density = LocalDensity.current
    val navigationBarHeight = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    return BottomBarContentClearance + navigationBarHeight + extra
}
