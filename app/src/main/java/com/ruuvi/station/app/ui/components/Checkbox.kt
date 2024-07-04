package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun ruuviCheckboxColors() = CheckboxDefaults.colors(
    checkedColor = RuuviStationTheme.colors.accent,
    uncheckedColor = RuuviStationTheme.colors.accent,
    checkmarkColor = RuuviStationTheme.colors.background,
    disabledColor = RuuviStationTheme.colors.inactive,
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RuuviCheckbox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    text: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            Checkbox(
                modifier = Modifier.padding(end = RuuviStationTheme.dimensions.extended),
                checked = checked,
                colors = ruuviCheckboxColors(),
                onCheckedChange = onCheckedChange,
            )
        }
        ClickableText(
            modifier = Modifier
                .fillMaxWidth(),
            text = AnnotatedString(text),
            style = RuuviStationTheme.typography.paragraph,
            onClick = {
                onCheckedChange(!checked)
            })
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RuuviCheckboxHypertext(
    modifier: Modifier = Modifier,
    checked: Boolean,
    text: String,
    linkText: List<String>,
    hyperlinks: List<String>,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            Checkbox(
                modifier = Modifier.padding(end = RuuviStationTheme.dimensions.extended),
                checked = checked,
                colors = ruuviCheckboxColors(),
                onCheckedChange = onCheckedChange,
            )
        }

        HyperlinkText(
            modifier = Modifier
                .fillMaxWidth(),
            fullText = text,
            linkText = linkText,
            hyperlinks = hyperlinks,
            textStyle = RuuviStationTheme.typography.paragraph,
            linkTextColor = RuuviStationTheme.typography.paragraph.color
        ) {
            onCheckedChange(!checked)
        }
    }
}