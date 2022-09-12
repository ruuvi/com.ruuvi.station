package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.dfu.ui.LoadingStatus

@Composable
fun ruuviButtonColors() = ButtonDefaults.buttonColors(
    backgroundColor = RuuviStationTheme.colors.accent,
    contentColor = RuuviStationTheme.colors.buttonText,
    disabledBackgroundColor = RuuviStationTheme.colors.inactive,
    disabledContentColor = RuuviStationTheme.colors.onInactive
)

@Composable
fun ruuviTextButtonColors() = ButtonDefaults.textButtonColors(
    contentColor = RuuviStationTheme.colors.accent,
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

@Composable
fun RuuviButton(
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(50),
    height: Dp = RuuviStationTheme.dimensions.buttonHeight,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier
            .height(height),
        enabled = enabled,
        shape = shape,
        colors = ruuviButtonColors(),
        elevation = ruuviButtonElevation(),
        contentPadding = PaddingValues(horizontal = RuuviStationTheme.dimensions.buttonInnerPadding),
        onClick = { onClick() }) {
        Text(
            text = text,
            style = RuuviStationTheme.typography.buttonText,
            textAlign = TextAlign.Center
        )
        if (loading) {
            Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.medium))
            LoadingStatus(color = RuuviStationTheme.colors.onInactive)
        }
    }
}

@Composable
fun RuuviTextButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TextButton(
        modifier = modifier,
        enabled = enabled,
        colors = ruuviTextButtonColors(),
        contentPadding = PaddingValues(horizontal = RuuviStationTheme.dimensions.buttonInnerPadding),
        onClick = { onClick() }) {
        Text(
            text = text,
            style = RuuviStationTheme.typography.textButtonText,
            textAlign = TextAlign.Center
        )
    }
}