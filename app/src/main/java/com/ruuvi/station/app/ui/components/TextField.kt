package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
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
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true
    )
}

@Composable
fun NumberTextFieldRuuvi(
    modifier: Modifier = Modifier,
    value: String,
    label: String? = null,
    hint: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    keyboardActions: KeyboardActions = KeyboardActions(),
    onValueChange: (Boolean, Double?) -> Unit,
    ) {
    val labelFun: @Composable (() -> Unit)? = if (label != null) { { Paragraph(text = label) } } else null
    val hintFun:@Composable (() -> Unit)? = if (hint != null) { { Paragraph(text = hint) } } else null

    var text by remember {
        mutableStateOf(value)
    }

    TextField(
        value = text,
        onValueChange = {
            val parsed = it.replace(',','.').toDoubleOrNull()
            if (it.isEmpty()){
                text = it
            } else {
                text = if (parsed != null || it == "-") it else text
            }
            onValueChange.invoke(parsed != null, parsed)
        },
        label = labelFun,
        placeholder = hintFun,
        textStyle = RuuviStationTheme.typography.paragraph,
        colors = RuuviTextFieldColors(),
        modifier = modifier,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}