package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun Progress(progress: Float) {
    LinearProgressIndicator(
        modifier = Modifier
            .fillMaxWidth(),
        progress = progress,
        color = RuuviStationTheme.colors.accent,
        backgroundColor = RuuviStationTheme.colors.trackColor
    )
}