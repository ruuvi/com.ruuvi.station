package com.ruuvi.station.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import timber.log.Timber
import kotlin.math.min

@Composable
fun TextUnit.scaledToMax(max: TextUnit): TextUnit {
    Timber.d("scaledToMax ${this.value} ${this.value.sp.value}")

    return if ((this.value * LocalDensity.current.fontScale) > max.value) {
        (max.value / LocalDensity.current.fontScale).sp
    } else {
        this
    }
}

@Composable
fun TextUnit.limitScaleTo(maxScale: Float): TextUnit {
    val systemFontScale = LocalDensity.current.fontScale
    val appliedScale = min(systemFontScale, maxScale)
    return (this / systemFontScale) * appliedScale
}

@Composable
fun Dp.scaleUpTo(maxScale: Float): Dp {
    val systemFontScale = LocalDensity.current.fontScale
    val appliedScale = min(systemFontScale, maxScale)
    return this * appliedScale
}

@Composable
fun Dp.toSp(): TextUnit {
    val density = LocalDensity.current
    return with(density) {
        val px = this@toSp.toPx()
        val scaledDensity = density.density * fontScale
        (px / scaledDensity).sp
    }
}