package com.ruuvi.station.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class RuuviDimensions(
    val minimal: Dp = 1.dp,
    val tiny: Dp = 2.dp,
    val small: Dp = 4.dp,
    val medium: Dp = 8.dp,
    val mediumPlus: Dp = 12.dp,
    val extended: Dp = 16.dp,
    val big: Dp = 24.dp,
    val extraBig: Dp = 32.dp,
    val huge: Dp = 64.dp,
    val screenPadding: Dp = extended,
    val textTopPadding: Dp = medium,
    val textBottomPadding: Dp = medium,
    val buttonHeight: Dp = 48.dp,
    val buttonWidth: Dp = 72.dp,
    val buttonHeightSmall: Dp = 40.dp,
    val buttonInnerPadding: Dp = big,
    val settingsListHeight: Dp = 40.dp,
)

val ruuviDimensions = RuuviDimensions()