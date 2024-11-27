package com.ruuvi.station.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import timber.log.Timber

@Composable
fun TextUnit.scaledToMax(max: TextUnit): TextUnit {
    Timber.d("scaledToMax ${this.value} ${this.value.sp.value}")

    return if ((this.value * LocalDensity.current.fontScale) > max.value) {
        (max.value / LocalDensity.current.fontScale).sp
    } else {
        this
    }
}