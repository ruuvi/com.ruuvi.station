package com.ruuvi.station.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import kotlinx.coroutines.delay

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

@Composable
fun LoadingScreen(status: String) {
    PageSurface() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Subtitle(text = status)
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
            LoadingStatus()
        }
    }
}

@Composable
fun LoadingAnimation3dots(
    circleColor: Color = RuuviStationTheme.colors.accent,
    circleSize: Dp = RuuviStationTheme.dimensions.extended,
    animationDelay: Int = 400,
    initialAlpha: Float = 0.3f
) {

    // 3 circles
    val circles = listOf(
        remember {
            Animatable(initialValue = initialAlpha)
        },
        remember {
            Animatable(initialValue = initialAlpha)
        },
        remember {
            Animatable(initialValue = initialAlpha)
        }
    )

    circles.forEachIndexed { index, animatable ->

        LaunchedEffect(Unit) {

            // Use coroutine delay to sync animations
            delay(timeMillis = (animationDelay / circles.size).toLong() * index)

            animatable.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = animationDelay
                    ),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    // container for circles
    Row(
        modifier = Modifier
        //.border(width = 2.dp, color = Color.Magenta)
    ) {

        // adding each circle
        circles.forEachIndexed { index, animatable ->

            // gap between the circles
            if (index != 0) {
                Spacer(modifier = Modifier.width(width = 6.dp))
            }

            Box(
                modifier = Modifier
                    .size(size = circleSize)
                    .clip(shape = CircleShape)
                    .background(
                        color = circleColor
                            .copy(alpha = animatable.value)
                    )
            ) {
            }
        }
    }
}