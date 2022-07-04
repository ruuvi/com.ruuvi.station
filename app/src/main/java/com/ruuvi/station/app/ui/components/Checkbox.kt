package com.ruuvi.station.app.ui.components

import androidx.compose.material.CheckboxDefaults
import androidx.compose.runtime.Composable
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun ruuviCheckboxColors() = CheckboxDefaults.colors(
    checkedColor = RuuviStationTheme.colors.accent,
    uncheckedColor = RuuviStationTheme.colors.accent,
    checkmarkColor = RuuviStationTheme.colors.background,
    disabledColor = RuuviStationTheme.colors.inactive,
)