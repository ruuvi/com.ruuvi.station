package com.ruuvi.station.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
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

@Composable
fun LoadingStatus(size: Dp = RuuviStationTheme.dimensions.big) {
    var currentRotation by remember { mutableStateOf(0f) }
    val rotation = remember { Animatable(currentRotation) }

    LaunchedEffect(1) {
        rotation.animateTo(
            targetValue = currentRotation + 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    Icon(
        Icons.Default.Refresh,
        contentDescription = "",
        modifier = Modifier
            .size(size)
            .rotate(rotation.value),
        tint = RuuviStationTheme.colors.accent
    )
}

@Composable
fun LoadingStatusDialog(size: Dp = RuuviStationTheme.dimensions.big) {
    Dialog(
        onDismissRequest = {}
    ) {
        LoadingStatus(size)
    }
}