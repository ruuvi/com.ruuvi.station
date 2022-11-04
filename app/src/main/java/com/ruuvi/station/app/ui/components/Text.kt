package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun Subtitle(text: String, modifier: Modifier = Modifier) {
    Text(
        style = RuuviStationTheme.typography.subtitle,
        text = text,
        modifier = modifier
    )
}

@Composable
fun SubtitleWithPadding(text: String, modifier: Modifier = Modifier) {
    Subtitle(
        text = text,
        modifier = modifier.padding(
            top = RuuviStationTheme.dimensions.textTopPadding,
            bottom = RuuviStationTheme.dimensions.textBottomPadding
        )
    )
}

@Composable
fun SmallerParagraph(text: String, modifier: Modifier = Modifier) {
    Text(
        style = RuuviStationTheme.typography.paragraphSmall,
        text = text,
        modifier = modifier
    )
}

@Composable
fun Paragraph(text: String, modifier: Modifier = Modifier) {
    Text(
        style = RuuviStationTheme.typography.paragraph,
        text = text,
        modifier = modifier
    )
}

@Composable
fun ParagraphWithPadding(text: String, modifier: Modifier = Modifier) {
    Paragraph(
        text = text,
        modifier = modifier.padding(
            top = RuuviStationTheme.dimensions.textTopPadding,
            bottom = RuuviStationTheme.dimensions.textBottomPadding
        )
    )
}

@Composable
fun WarningText(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        style = RuuviStationTheme.typography.warning,
        text = text
    )
}

@Composable
fun WarningWithPadding(text: String, modifier: Modifier = Modifier) {
    WarningText(
        text = text,
        modifier.padding(
            top = RuuviStationTheme.dimensions.textTopPadding,
            bottom = RuuviStationTheme.dimensions.textBottomPadding
        )
    )
}