package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.dfu.ui.LoadingStatus

@Composable
fun RuuviButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier
            .height(RuuviStationTheme.dimensions.buttonHeight),
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = ruuviButtonColors(),
        elevation = ruuviButtonElevation(),
        onClick = { onClick() }) {
        Row(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.buttonInnerPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, style = RuuviStationTheme.typography.buttonText)
            if (loading) {
                Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.medium))
                LoadingStatus(color = RuuviStationTheme.colors.onInactive)
            }
        }
    }
}

@Composable
fun ruuviButtonColors() = ButtonDefaults.buttonColors(
    backgroundColor = RuuviStationTheme.colors.accent,
    contentColor = RuuviStationTheme.colors.buttonText,
    disabledBackgroundColor = RuuviStationTheme.colors.inactive,
    disabledContentColor = RuuviStationTheme.colors.onInactive
)

@Composable
fun ruuviButtonElevation() = ButtonDefaults.elevation(
    defaultElevation = 0.dp,
    pressedElevation = 0.dp,
    disabledElevation = 0.dp,
    hoveredElevation = 0.dp,
    focusedElevation = 0.dp
)
