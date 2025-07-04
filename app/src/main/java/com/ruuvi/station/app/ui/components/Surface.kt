package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun PageSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        color = RuuviStationTheme.colors.background,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        content.invoke()
    }
}

@Composable
fun PageSurfaceWithPadding(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    PageSurface(modifier.padding(RuuviStationTheme.dimensions.screenPadding)) {
        content.invoke()
    }
}

@Composable
fun DividerRuuvi() {
    Divider(color = RuuviStationTheme.colors.divider)
}

@Composable
fun DividerSurfaceColor() {
    Divider(color = RuuviStationTheme.colors.background)
}


@Composable
fun StatusBarFill(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RuuviStationTheme.colors.topBar)
    ) {
        content.invoke()
    }
}

