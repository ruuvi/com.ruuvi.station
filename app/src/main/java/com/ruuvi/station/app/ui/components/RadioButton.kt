package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun ruuviRadioButtonColors() = RadioButtonDefaults.colors(
    selectedColor = RuuviStationTheme.colors.accent,
    unselectedColor = RuuviStationTheme.colors.accent,
    disabledColor = RuuviStationTheme.colors.inactive,
)

@Composable
fun RadioButtonRuuvi(
    text: String,
    isSelected: Boolean,
    onClick: (() -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick?.invoke() }
    ) {
        RadioButton(
            selected = (isSelected),
            colors = ruuviRadioButtonColors(),
            onClick = { onClick?.invoke() }
        )

        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = text,
            style = RuuviStationTheme.typography.paragraph
        )
    }
}