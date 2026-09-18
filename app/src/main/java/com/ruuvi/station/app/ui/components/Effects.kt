package com.ruuvi.station.app.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay

@Composable
fun BlinkingEffect(
    content: @Composable () -> Unit
) {
    var contentVisible by remember { mutableStateOf(true) }

    val alpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(250),
        label = "blinkAlpha"
    )

    Box(Modifier.alpha(alpha)) {
        content()
    }

    LaunchedEffect(Unit) {
        while (true) {
            val time = System.currentTimeMillis()
            contentVisible = (time % 1000) < 500
            delay(75)
        }
    }
}

@Composable
fun blinkingAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "alert border pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1_000,
                easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f),
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alert border alpha",
    )
    return alpha
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
