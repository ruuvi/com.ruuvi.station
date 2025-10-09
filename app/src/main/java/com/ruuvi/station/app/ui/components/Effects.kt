package com.ruuvi.station.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.*
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
fun blinkingAlpha(
    blinkingEffect: (Long) -> Float = ::fadeBlinking
): Float {
    var blinkingAlpha by remember { mutableStateOf(1f) }

    LaunchedEffect(true) {
        while (true) {
            val time = System.currentTimeMillis()
            blinkingAlpha = blinkingEffect(time)
            delay(50)
        }
    }
    return blinkingAlpha
}

fun fadeBlinking(time: Long): Float {
    return when (val value = time % 2000) {
        in 0..100 -> 0.3f
        in 101..700 -> 0.3f + (value - 100) / 600f * 0.7f
        in 701..1300 -> 1f
        in 1301..1900 -> 1f - (value - 1300) / 600f * 0.7f
        in 1901..2000 -> 0.3f
        else -> 0.3f
    }
}

fun onOffBlinking(time: Long): Float {
    val value = time % 1000
    return when {
        value <= 0 -> 0f
        value in 0..350 -> value / 350f
        value in 351..650 -> 1f
        value in 651..999 -> 1f - ((value - 650) / 350f)
        else -> 0f
    }
}
