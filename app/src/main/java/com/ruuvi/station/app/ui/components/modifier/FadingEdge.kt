package com.ruuvi.station.app.ui.components.modifier

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.ruuvi.station.util.ui.BottomFadingEdgeDefaults

/** Font-scaled fade covering at least two body-text lines above the navigation bar. */
@Composable
internal fun defaultBottomFadeLength(): Dp {
    val density = LocalDensity.current
    val navigationBarInset = WindowInsets.navigationBars.getBottom(density)
    return with(density) {
        BottomFadingEdgeDefaults.LENGTH_SP.sp.toDp() + navigationBarInset.toDp()
    }
}

/**
 * Apply before verticalScroll so the fade stays at the viewport's bottom edge.
 * Bottom inset padding belongs inside the scrollable content.
 */
@Composable
fun Modifier.bottomFadingEdge(scrollState: ScrollState, length: Dp = defaultBottomFadeLength()) =
    bottomFadingEdge(length) { scrollState.canScrollForward }

@Composable
fun Modifier.bottomFadingEdge(lazyListState: LazyListState, length: Dp = defaultBottomFadeLength()) =
    bottomFadingEdge(length) { lazyListState.canScrollForward }

@Composable
fun Modifier.bottomFadingEdge(lazyGridState: LazyGridState, length: Dp = defaultBottomFadeLength()) =
    bottomFadingEdge(length) { lazyGridState.canScrollForward }

@Composable
fun Modifier.bottomFadingEdge(lazyStaggeredGridState: LazyStaggeredGridState, length: Dp = defaultBottomFadeLength()) =
    bottomFadingEdge(length) { lazyStaggeredGridState.canScrollForward }

private fun Modifier.bottomFadingEdge(length: Dp, canScrollForward: () -> Boolean) = this
    .graphicsLayer {
        compositingStrategy = if (canScrollForward()) {
            CompositingStrategy.Offscreen
        } else {
            CompositingStrategy.Auto
        }
    }
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

private val topFade = Brush.verticalGradient(
    0f to Color.Transparent,
    0.05f to Color.White,
    1f to Color.White,
)

@Composable
fun Modifier.fadingEdge(
    scrollState: ScrollState,
    bottomFadeLength: Dp = defaultBottomFadeLength(),
) = fadingEdge(
    bottomFadeLength = bottomFadeLength,
    canScrollBackward = { scrollState.canScrollBackward },
    canScrollForward = { scrollState.canScrollForward },
)

@Composable
fun Modifier.fadingEdge(
    lazyListState: LazyListState,
    bottomFadeLength: Dp = defaultBottomFadeLength(),
) = fadingEdge(
    bottomFadeLength = bottomFadeLength,
    canScrollBackward = { lazyListState.canScrollBackward },
    canScrollForward = { lazyListState.canScrollForward },
)

private fun Modifier.fadingEdge(
    bottomFadeLength: Dp,
    canScrollBackward: () -> Boolean,
    canScrollForward: () -> Boolean,
) = this
    .graphicsLayer {
        compositingStrategy = if (canScrollBackward() || canScrollForward()) {
            CompositingStrategy.Offscreen
        } else {
            CompositingStrategy.Auto
        }
    }
    .drawWithCache {
        val fadeHeight = bottomFadeLength.toPx().coerceIn(0f, size.height)
        val bottomFadeStart = if (size.height > 0f) 1f - fadeHeight / size.height else 1f
        val bottomFade = Brush.verticalGradient(
            0f to Color.White,
            bottomFadeStart to Color.White,
            1f to Color.Transparent,
        )
        val topBottomFade = Brush.verticalGradient(
            0f to Color.Transparent,
            minOf(0.05f, bottomFadeStart) to Color.White,
            bottomFadeStart to Color.White,
            1f to Color.Transparent,
        )
        onDrawWithContent {
            drawContent()
            val notTop = canScrollBackward()
            val notBottom = canScrollForward() && fadeHeight > 0f
            val fadeBrush = when {
                notTop && notBottom -> topBottomFade
                notTop -> topFade
                notBottom -> bottomFade
                else -> null
            }
            fadeBrush?.let {
                drawRect(brush = it, blendMode = BlendMode.DstIn)
            }
        }
    }