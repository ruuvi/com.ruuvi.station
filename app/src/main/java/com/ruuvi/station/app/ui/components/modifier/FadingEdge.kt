package com.ruuvi.station.app.ui.components.modifier

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ruuvi.station.util.ui.BottomFadingEdgeDefaults

/**
 * Apply before verticalScroll so the fade stays at the viewport's bottom edge.
 * Bottom inset padding belongs inside the scrollable content.
 */
fun Modifier.bottomFadingEdge(scrollState: ScrollState, length: Dp = BottomFadingEdgeDefaults.LENGTH_DP.dp) =
    bottomFadingEdge(length) { scrollState.canScrollForward }

fun Modifier.bottomFadingEdge(lazyListState: LazyListState, length: Dp = BottomFadingEdgeDefaults.LENGTH_DP.dp) =
    bottomFadingEdge(length) { lazyListState.canScrollForward }

fun Modifier.bottomFadingEdge(lazyGridState: LazyGridState, length: Dp = BottomFadingEdgeDefaults.LENGTH_DP.dp) =
    bottomFadingEdge(length) { lazyGridState.canScrollForward }

private fun Modifier.bottomFadingEdge(length: Dp, canScrollForward: () -> Boolean) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithCache {
        val fadeHeight = length.toPx().coerceIn(0f, size.height)
        val fadeBrush = if (fadeHeight > 0f) {
            Brush.verticalGradient(
                colors = listOf(Color.White, Color.Transparent),
                startY = size.height - fadeHeight,
                endY = size.height,
            )
        } else {
            null
        }
        onDrawWithContent {
            drawContent()
            if (canScrollForward() && fadeBrush != null) {
                drawRect(brush = fadeBrush, blendMode = BlendMode.DstIn)
            }
        }
    }

val topBottomFade = Brush.verticalGradient(
    0f to Color.Transparent,
    0.05f to Color.White,
    0.95f to Color.White,
    1f to Color.Transparent
)

val topFade = Brush.verticalGradient(
    0f to Color.Transparent,
    0.05f to Color.White,
    0.95f to Color.White,
    1f to Color.White
)

val bottomFade = Brush.verticalGradient(
    0f to Color.White,
    0.05f to Color.White,
    0.95f to Color.White,
    1f to Color.Transparent
)

fun Modifier.fadingEdge(scrollState: ScrollState) = this
    .graphicsLayer {
        compositingStrategy = if (scrollState.maxValue > 0) {
            CompositingStrategy.Offscreen
        } else {
            CompositingStrategy.Auto
        }
    }
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

fun Modifier.fadingEdge(lazyListState: LazyListState) = this
    .graphicsLayer {
        compositingStrategy = if (lazyListState.canScrollBackward || lazyListState.canScrollForward) {
            CompositingStrategy.Offscreen
        } else {
            CompositingStrategy.Auto
        }
    }
    .drawWithContent {
        drawContent()

        val notTop = lazyListState.canScrollBackward
        val notBottom = lazyListState.canScrollForward

        val fadeBrush = when {
            notTop && notBottom -> topBottomFade
            notTop -> topFade
            notBottom -> bottomFade
            else -> null
        }

        fadeBrush?.let {
            drawRect(brush = fadeBrush, blendMode = BlendMode.DstIn)
        }
    }