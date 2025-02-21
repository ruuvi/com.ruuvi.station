package com.ruuvi.station.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize

@Composable
fun LimitedScale(): Float {
    return LocalDensity.current.fontScale.coerceAtMost(1.5f)
}

@Composable
fun Scale(): Float {
    return LocalDensity.current.fontScale
}

@Composable
fun DpSize.limitedScale(): DpSize {
    return this * LimitedScale()
}

@Composable
fun DpSize.scale(): DpSize {
    return this * Scale()
}

@Composable
fun Dp.limitedScale(): Dp {
    return this * LimitedScale()
}

@Composable
fun Dp.scale(): Dp {
    return this * Scale()
}