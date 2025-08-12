package com.ruuvi.station.app.ui.components.modifier

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

val topBottomFade = Brush.verticalGradient(
    0f to Color.Transparent,
    0.08f to Color.White,
    0.92f to Color.White,
    1f to Color.Transparent
)

val topFade = Brush.verticalGradient(
    0f to Color.Transparent,
    0.08f to Color.White,
    0.92f to Color.White,
    1f to Color.White
)

val bottomFade = Brush.verticalGradient(
    0f to Color.White,
    0.08f to Color.White,
    0.92f to Color.White,
    1f to Color.Transparent
)

fun Modifier.fadingEdge(scrollState: ScrollState) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fadeBrush = when {
            scrollState.maxValue == 0 -> null
            scrollState.value == scrollState.maxValue -> topFade
            scrollState.value == 0 -> bottomFade
            else -> topBottomFade
        }
        fadeBrush?.let {
            drawRect(brush = fadeBrush, blendMode = BlendMode.DstIn)
        }
    }