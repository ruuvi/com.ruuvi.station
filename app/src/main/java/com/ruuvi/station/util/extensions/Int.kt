package com.ruuvi.station.util.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Int.fixedSp(): TextUnit {
    return with(LocalDensity.current) {
        this@fixedSp.dp.toSp()
    }
}

val Int.fixedSp: TextUnit
    @Composable get() =  fixedSp()


@Composable
fun Int.limitedSp(maxScaleFactor: Float = 1.5f): TextUnit {
    val systemFontScale = LocalDensity.current.fontScale
    val effectiveFontScale = systemFontScale.coerceAtMost(maxScaleFactor)

    return (this / systemFontScale * effectiveFontScale).sp
}

val Int.limitedSp: TextUnit
    @Composable get() = limitedSp()