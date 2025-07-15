package com.ruuvi.station.tagdetails.ui.elements

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruuvi.station.app.ui.components.limitScaleTo
import com.ruuvi.station.app.ui.components.scaleUpTo
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.White80
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.units.model.getDescriptionBodyResId
import org.apache.commons.lang3.StringEscapeUtils
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValueBottomSheet (
    sheetValue: EnvironmentValue,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        containerColor = RuuviStationTheme.colors.sensorValueBottomSheetBackground,
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.7f))
            )
        },
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        ValueSheetContent(sheetValue)
    }
}

@Composable
fun ValueSheetContent(
    sheetValue: EnvironmentValue
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val maxHeight = screenHeight * 0.75f

    Column (
        modifier = Modifier
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
    ) {
        ValueSheetHeader(sheetValue)
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        ValueSheetDescription(sheetValue)
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
    }
}

@Composable
fun ValueSheetDescription(
    sheetValue: EnvironmentValue,
    modifier: Modifier = Modifier
) {
    MarkupText(sheetValue.unitType.getDescriptionBodyResId())
//    Column {
//        ValueSheetHeaderText(stringResource(sheetValue.unitType.getDescriptionHeaderResId()))
//        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
//        Paragraph(text = stringResource(sheetValue.unitType.getDescriptionBodyResId()))
//    }
}

@Composable
fun ValueSheetHeader(
    sheetValue: EnvironmentValue,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {

        Icon(
            modifier = Modifier
                .height(24.dp.scaleUpTo(1.5f))
                .padding(end = RuuviStationTheme.dimensions.medium),
            painter = painterResource(id = sheetValue.unitType.iconRes),
            tint = Color(0xff5ebdb2),
            contentDescription = ""
        )

        ValueSheetHeaderText(
            modifier = Modifier
                .alignByBaseline(),
            text = stringResource(sheetValue.unitType.measurementTitle)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ValueSheetHeaderText(
                modifier = Modifier
                    .alignByBaseline(),
                text = sheetValue.valueWithoutUnit
            )

            ValueSheetUnitText(
                modifier = Modifier
                    .alignByBaseline()
                    .padding(
                        start = RuuviStationTheme.dimensions.small
                    ),
                text = sheetValue.unitString
            )
        }
    }
}

@Composable
fun ValueSheetHeaderText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        fontSize = RuuviStationTheme.fontSizes.normal.limitScaleTo(1.5f),
        fontFamily = ruuviStationFonts.montserratBold,
        fontWeight = FontWeight.Bold,
        text = text,
        color = Color.White
    )
}

@Composable
fun ValueSheetUnitText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        style = RuuviStationTheme.typography.dashboardSecondary,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = RuuviStationTheme.fontSizes.miniature.limitScaleTo(1.5f),
        text = text,
        maxLines = 1
    )
}

@Composable
fun MarkupText(@StringRes textRes: Int) {
    val rawEscaped = stringResource(id = textRes)
    val raw = StringEscapeUtils.unescapeHtml4(rawEscaped)
    Timber.d("raw $raw")
    
    val annotatedString = remember(raw) {
        parseInlineStyledText(
            input = raw,
            tagStyles = mapOf(
                "title" to SpanStyle(
                    fontSize = 16.sp,
                    fontFamily = ruuviStationFonts.montserratBold,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            ),
            defaultStyle = SpanStyle(
                color = White80,
                fontFamily = ruuviStationFonts.mulishRegular,
                fontSize = ruuviStationFontsSizes.normal
            )
        )
    }

    Text(text = annotatedString)
}

fun parseInlineStyledText(
    input: String,
    tagStyles: Map<String, SpanStyle>,
    newlineAfterTags: Set<String> = setOf(),// = setOf("title", "subtitle"),
    defaultStyle: SpanStyle? = null
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val tagRegex = Regex("""<(\/?)(\w+)>""")
    val tagStack = mutableListOf<Pair<String, Int>>()

    var currentIndex = 0
    var lastIndex = 0

    val matches = tagRegex.findAll(input)

    for (match in matches) {
        val start = match.range.first
        val end = match.range.last + 1
        val isClosing = match.groupValues[1] == "/"
        val tag = match.groupValues[2]

        // Text before tag
        if (start > lastIndex) {
            val text = input.substring(lastIndex, start)
            builder.append(text)
            if (tagStack.isEmpty() && defaultStyle != null) {
                builder.addStyle(defaultStyle, currentIndex, currentIndex + text.length)
            }
            currentIndex += text.length
        }

        if (!isClosing) {
            tagStack.add(tag to currentIndex)
        } else {
            val openIndex = tagStack.indexOfLast { it.first == tag }
            if (openIndex != -1) {
                val (openTag, startOffset) = tagStack.removeAt(openIndex)
                tagStyles[openTag]?.let { style ->
                    builder.addStyle(style, startOffset, currentIndex)
                }

                if (tag in newlineAfterTags) {
                    builder.append("\n")
                    currentIndex += 1
                }
            }
        }

        lastIndex = end
    }

    // Remaining text after last tag
    if (lastIndex < input.length) {
        val text = input.substring(lastIndex)
        builder.append(text)
        if (tagStack.isEmpty() && defaultStyle != null) {
            builder.addStyle(defaultStyle, currentIndex, currentIndex + text.length)
        }
    }

    return builder.toAnnotatedString()
}



@Preview
@Composable
private fun ValueBottomSheet() {
    val value = EnvironmentValue(
        original = 22.50,
        value = 22.50,
        accuracy = Accuracy.Accuracy1,
        valueWithUnit = "22.5 %",
        valueWithoutUnit = "22.5",
        unitString = "%",
        unitType = UnitType.HumidityUnit.Relative
    )

    RuuviTheme {
        Surface(color = RuuviStationTheme.colors.sensorValueBottomSheetBackground) {
            ValueSheetContent(sheetValue = value)
        }
    }
}