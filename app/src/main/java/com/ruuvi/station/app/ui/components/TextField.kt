
package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun RuuviTextFieldColors() = TextFieldDefaults.textFieldColors(
    backgroundColor = RuuviStationTheme.colors.background,
    cursorColor = RuuviStationTheme.colors.accent,
    trailingIconColor = RuuviStationTheme.colors.accent,
    focusedIndicatorColor = RuuviStationTheme.colors.accent,
    unfocusedIndicatorColor = RuuviStationTheme.colors.trackColor
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TextFieldRuuvi(
    modifier: Modifier = Modifier,
    value: String,
    hint: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    onValueChange: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val customTextSelectionColors = TextSelectionColors(
        handleColor = RuuviStationTheme.colors.accent,
        backgroundColor = RuuviStationTheme.colors.accent.copy(alpha = 0.4f)
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = RuuviStationTheme.typography.paragraph,
            interactionSource = interactionSource,
            modifier = modifier.indicatorLine(
                enabled = true,
                colors = RuuviTextFieldColors(),
                interactionSource = interactionSource,
                isError = false
            ),            visualTransformation = VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            cursorBrush = SolidColor(RuuviStationTheme.colors.accent),
            keyboardActions = keyboardActions,
            singleLine = true
        ) { innerTextField ->
            TextFieldDefaults.TextFieldDecorationBox(
                value = value,
                enabled = true,
                placeholder = { Hint(hint) },
                visualTransformation = VisualTransformation.None,
                innerTextField = innerTextField,
                colors = RuuviTextFieldColors(),
                singleLine = true,
                interactionSource = interactionSource,
                contentPadding = PaddingValues(vertical = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TextFieldRuuvi(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    hint: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    onValueChange: (TextFieldValue) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val customTextSelectionColors = TextSelectionColors(
        handleColor = RuuviStationTheme.colors.accent,
        backgroundColor = RuuviStationTheme.colors.accent.copy(alpha = 0.4f)
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = RuuviStationTheme.typography.paragraph,
            interactionSource = interactionSource,
            modifier = modifier.indicatorLine(
                enabled = true,
                colors = RuuviTextFieldColors(),
                interactionSource = interactionSource,
                isError = false
            ),            visualTransformation = VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            cursorBrush = SolidColor(RuuviStationTheme.colors.accent),
            keyboardActions = keyboardActions,
            singleLine = true)
        { innerTextField ->
            TextFieldDefaults.TextFieldDecorationBox(
                value = value.text,
                enabled = true,
                placeholder = { Hint(hint) },
                visualTransformation = VisualTransformation.None,
                innerTextField = innerTextField,
                colors = RuuviTextFieldColors(),
                singleLine = true,
                interactionSource = interactionSource,
                contentPadding = PaddingValues(vertical = 8.dp),
            )
        }
    }
}

@Composable
fun NumberTextFieldRuuvi(
    modifier: Modifier = Modifier,
    value: String,
    hint: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    keyboardActions: KeyboardActions = KeyboardActions(),
    onValueChange: (Boolean, Double?) -> Unit,
    ) {

    var text by remember {
        mutableStateOf(value)
    }

    TextFieldRuuvi(
        modifier = modifier,
        value = text,
        hint = hint,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        onValueChange = {
            val parsed = it.replace(',','.').toDoubleOrNull()
            if (it.isEmpty()){
                text = it
            } else {
                text = if (parsed != null || it == "-") it else text
            }
            onValueChange.invoke(parsed != null, parsed)
        }
    )
}

@Composable
fun Hint(hint: String?) {
    if (hint != null) {
        Text(
            style = RuuviStationTheme.typography.paragraph,
            text = hint,
            color = RuuviStationTheme.colors.primary.copy(alpha = 0.5f)
        )
    }
}