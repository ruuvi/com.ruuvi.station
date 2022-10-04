package com.ruuvi.station.app.ui.components

import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun RuuviTextFieldColors() = TextFieldDefaults.textFieldColors(
    backgroundColor = RuuviStationTheme.colors.background,
    cursorColor = RuuviStationTheme.colors.accent,
    trailingIconColor = RuuviStationTheme.colors.accent,
    focusedIndicatorColor = RuuviStationTheme.colors.accent,
    unfocusedIndicatorColor = RuuviStationTheme.colors.trackColor
)

@Composable
fun TextFieldRuuvi(
    modifier: Modifier = Modifier,
    value: String,
    label: String? = null,
    hint: String? = null,
    onValueChange: (String) -> Unit,
) {
    val labelFun: @Composable (() -> Unit)? = if (label != null) { { Paragraph(text = label) } } else null
    val hintFun:@Composable (() -> Unit)? = if (hint != null) { { Paragraph(text = hint) } } else null

    TextField(
        value = value,
        onValueChange = onValueChange,
        label = labelFun,
        placeholder = hintFun,
        textStyle = RuuviStationTheme.typography.paragraph,
        colors = RuuviTextFieldColors(),
        modifier = modifier,
        singleLine = true
    )
}