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
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        content()
    }

    LaunchedEffect(key1 = 1) {
        while (true) {
            delay(500)
            contentVisible = !contentVisible
        }
    }
}