package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.util.text.getDecimalMatches

@Composable
fun TextEditButton(
    modifier: Modifier = Modifier,
    value: String?,
    emptyText: String,
    textAlign: TextAlign = TextAlign.Start,
    applyBoldStyleToDecimals: Boolean = false,
    editAction: () -> Unit
) {
    Row(modifier = modifier
        .clickable { editAction.invoke() }
        .defaultMinSize(minHeight = RuuviStationTheme.dimensions.sensorSettingTitleHeight)
        .padding(vertical = RuuviStationTheme.dimensions.mediumPlus),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            style = RuuviStationTheme.typography.paragraph,
            textAlign = textAlign,
            text = if (value.isNullOrEmpty()) {
                AnnotatedString(emptyText)
            } else {
                if (applyBoldStyleToDecimals) {
                    applyStyleToDecimals(value)
                } else {
                    AnnotatedString(value)
                }
            }
        )

        Image(
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            painter = painterResource(id = R.drawable.edit_20),
            contentDescription = ""
        )
    }
}

fun applyStyleToDecimals(
    text: String,
    spanStyle: SpanStyle = SpanStyle(fontWeight = FontWeight.Bold)
): AnnotatedString {
    val matches = text.getDecimalMatches()

    return buildAnnotatedString {
        append(text)
        for (match in matches) {
            addStyle(
                spanStyle,
                match.range.first,
                match.range.last + 1
            )
        }
    }
}

@Composable
fun TextEditWithCaptionButton(
    value: String? = null,
    title: String,
    icon: Painter,
    tint: Color,
    editAction: () -> Unit
) {
    Row(modifier = Modifier
        .clickable { editAction.invoke() }
        .defaultMinSize(minHeight = RuuviStationTheme.dimensions.sensorSettingTitleHeight)
        .padding(vertical = RuuviStationTheme.dimensions.mediumPlus),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            style = RuuviStationTheme.typography.subtitle,
            textAlign = TextAlign.Start,
            text = title)
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            style = RuuviStationTheme.typography.paragraph,
            textAlign = TextAlign.End,
            text = value ?: "")
        Icon(
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            painter = icon,
            tint = tint,
            contentDescription = ""
        )
    }
}

@Composable
fun TextWithCaption(
    value: String? = null,
    title: String
) {
    Row(modifier = Modifier
        .defaultMinSize(minHeight = RuuviStationTheme.dimensions.sensorSettingTitleHeight)
        .padding(vertical = RuuviStationTheme.dimensions.mediumPlus),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            style = RuuviStationTheme.typography.subtitle,
            textAlign = TextAlign.Start,
            text = title)
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            style = RuuviStationTheme.typography.paragraph,
            textAlign = TextAlign.End,
            text = value ?: "")
    }
}