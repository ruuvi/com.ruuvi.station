package com.ruuvi.station.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun BlinkingEffect(
    content: @Composable () -> Unit
) {
    var contentVisible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = contentVisible,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(250))
    ) {
        content()
    }

    LaunchedEffect(key1 = 1) {
        while (true) {
            val time = System.currentTimeMillis()
            contentVisible = (time % 1000) < 500
            delay(75)
        }
    }
}

@Composable
fun blinkingAlpha(): Float {
    var blinkingAlpha by remember { mutableStateOf(1f) }

    LaunchedEffect(true) {
        while (true) {
            val time = System.currentTimeMillis()
            blinkingAlpha = mapValueToUnitRange((time % 1000).toInt())
            delay(75)
        }
    }
    return blinkingAlpha
}

fun mapValueToUnitRange(value: Int): Float {
    return when {
        value <= 0 -> 0f
        value in 0..350 -> value / 350f
        value in 351..650 -> 1f
        value in 651..1000 -> 1f - ((value - 650) / 350f)
        else -> 0f
    }
}