package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun ruuviSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = RuuviStationTheme.colors.accent,
    uncheckedThumbColor = RuuviStationTheme.colors.inactive,
    checkedTrackColor = RuuviStationTheme.colors.trackColor,
    uncheckedTrackColor = RuuviStationTheme.colors.trackInactive,
    checkedTrackAlpha = 1f,
    uncheckedTrackAlpha = 1f
)

@Composable
fun SwitchRuuvi (
    text: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onCheckedChange?.invoke(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Subtitle(
            text = text,
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Switch(
                checked = checked,
                colors = ruuviSwitchColors(),
                onCheckedChange = onCheckedChange,
                modifier = Modifier.width(IntrinsicSize.Min)
            )
        }
    }
}