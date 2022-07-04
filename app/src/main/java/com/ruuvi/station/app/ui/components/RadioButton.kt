package com.ruuvi.station.app.ui.components

import androidx.compose.material.RadioButtonDefaults
import androidx.compose.runtime.Composable
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun ruuviRadioButtonColors() = RadioButtonDefaults.colors(
    selectedColor = RuuviStationTheme.colors.accent,
    unselectedColor = RuuviStationTheme.colors.accent,
    disabledColor = RuuviStationTheme.colors.inactive,
)