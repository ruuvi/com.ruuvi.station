package com.ruuvi.station.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes
import org.apache.commons.lang3.StringEscapeUtils
import timber.log.Timber

@Composable
fun MarkupText(@StringRes textRes: Int) {
    val rawEscaped = stringResource(id = textRes)
    val raw = StringEscapeUtils.unescapeHtml4(rawEscaped)
    Timber.d("raw $raw")

    val linkColor = RuuviStationTheme.colors.accent
    val headerColor = RuuviStationTheme.colors.popupHeaderText
    val textColor = RuuviStationTheme.colors.primary

    val parsed = remember(raw) {
        parseModernMarkup(
            input = raw,
            tagStyles = mapOf(
                "title" to SpanStyle(
                    fontSize = ruuviStationFontsSizes.normal,
                    fontFamily = ruuviStationFonts.mulishBold,
                    fontWeight = FontWeight.Bold,
                    color = headerColor
                ),
                "b" to SpanStyle(
                    fontSize = ruuviStationFontsSizes.compact,
                    fontFamily = ruuviStationFonts.mulishBold,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                ),
                "link" to SpanStyle(
                    color = linkColor,
                    fontFamily = ruuviStationFonts.mulishBold,
                    fontSize = ruuviStationFontsSizes.compact,
                    textDecoration = TextDecoration.Underline
                )
            ),
            defaultStyle = SpanStyle(
                color = textColor,
                fontFamily = ruuviStationFonts.mulishRegular,
                fontSize = ruuviStationFontsSizes.compact
            ),

        )
    }

    BasicText(text = parsed)
}

fun parseModernMarkup(
    input: String,
    tagStyles: Map<String, SpanStyle>,
    defaultStyle: SpanStyle? = null
): AnnotatedString {
    val builder = AnnotatedString.Builder()


    var cursor = 0
    val tagRegex = Regex("""\[(\w+)(?:\s+url\s*=\s*(?:"([^"]+)"|([^\]\s]+)))?]""")

    while (cursor < input.length) {
        val match = tagRegex.find(input, cursor)
        if (match == null) {
            // Append remaining text
            val remaining = input.substring(cursor)
            if (remaining.isNotEmpty()) {
                defaultStyle?.let { builder.pushStyle(it) }
                builder.append(remaining)
                if (defaultStyle != null) builder.pop()
            }
            break
        }

        val tagStart = match.range.first
        val tagEnd = match.range.last + 1
        val tag = match.groupValues[1]
        val url = match.groups[2]?.value?.takeIf { it.isNotBlank() }
            ?: match.groups[3]?.value?.takeIf { it.isNotBlank() }

        // Append plain text before tag
        if (tagStart > cursor) {
            val plain = input.substring(cursor, tagStart)
            defaultStyle?.let { builder.pushStyle(it) }
            builder.append(plain)
            if (defaultStyle != null) builder.pop()
        }

        // Find corresponding closing tag
        val closingTag = "[/$tag]"
        val closeIndex = input.indexOf(closingTag, tagEnd)
        if (closeIndex == -1) {
            // Malformed tag: treat as plain text
            val fallback = input.substring(tagStart, tagEnd)
            defaultStyle?.let { builder.pushStyle(it) }
            builder.append(fallback)
            if (defaultStyle != null) builder.pop()
            cursor = tagEnd
            continue
        }

        val content = input.substring(tagEnd, closeIndex)
        val style = tagStyles[tag]

        when {
            tag == "link" && url != null -> {
                builder.withLink(
                    LinkAnnotation.Url(url, TextLinkStyles(style = style ?: SpanStyle())),
                ) {
                    append(content)
                }
            }
            style != null -> {
                builder.pushStyle(style)
                builder.append(content)
                builder.pop()
            }
            else -> {
                // Unknown tag, fallback to default style
                defaultStyle?.let { builder.pushStyle(it) }
                builder.append(content)
                if (defaultStyle != null) builder.pop()
            }
        }

        cursor = closeIndex + closingTag.length
    }

    return builder.toAnnotatedString()
}
