package com.ruuvi.station.util.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun Dp.dpToPx() = with(LocalDensity.current) { this@dpToPx.toPx() }

@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

@Composable
fun Float.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

@Composable
fun Int.mmToDp(): Dp {
    val density = LocalDensity.current
    val context = LocalContext.current
    val metrics = context.resources.displayMetrics
    val pxPerMm = metrics.xdpi / 25.4f
    return (this * pxPerMm).pxToDp()
}