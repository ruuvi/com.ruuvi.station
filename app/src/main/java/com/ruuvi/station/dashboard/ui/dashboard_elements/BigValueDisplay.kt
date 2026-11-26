package com.ruuvi.station.dashboard.ui.dashboard_elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Top
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.limitScaleTo
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import kotlin.math.roundToInt

@Composable
fun BigValueDisplay(
    value: EnvironmentValue,
    alertTriggered: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (alertTriggered) {
        RuuviStationTheme.colors.activeAlertThemed
    } else {
        RuuviStationTheme.colors.settingsTitleText
    }

    Row(
        modifier = modifier.wrapContentSize(),
        verticalAlignment = Top
    ) {
        Text(
            modifier = Modifier,
            style = RuuviStationTheme.typography.dashboardBigValue,
            text = value.valueWithoutUnit,
            fontSize = RuuviStationTheme.fontSizes.huge.limitScaleTo(1.5f),
            color = textColor
        )

        Text(
            modifier = Modifier
                .offset(y = 7.dp)
                .padding(start = 4.dp),
            style = RuuviStationTheme.typography.dashboardBigValueUnit,
            fontSize = RuuviStationTheme.fontSizes.compact.limitScaleTo(1.5f),
            text = value.unitString,
            color = RuuviStationTheme.colors.settingsTitleText
        )

    }
}

@Composable
fun BigValueExtDisplay(
    value: EnvironmentValue,
    alertTriggered: Boolean,
    showTitle: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (alertTriggered) {
        RuuviStationTheme.colors.activeAlertThemed
    } else {
        RuuviStationTheme.colors.dashboardValue
    }

    val bigValueFontSize = RuuviStationTheme.fontSizes.huge.limitScaleTo(1.5f)
    val unitFontSize = RuuviStationTheme.fontSizes.compact.limitScaleTo(1.1f)
    val titleFontSize = RuuviStationTheme.fontSizes.petite.limitScaleTo(1.1f)

    BigValueLayout(
        bigValueFontSize = bigValueFontSize,
        unitFontSize = unitFontSize,
        modifier = modifier,
        bigValue = {
            Text(
                style = RuuviStationTheme.typography.dashboardBigValue,
                text = value.valueWithoutUnit,
                fontSize = bigValueFontSize,
                color = textColor,
            )
        },
        unit = {
            Text(
                style = RuuviStationTheme.typography.dashboardBigValueUnit,
                fontSize = unitFontSize,
                text = value.unitString,
            )
        },
        title = if (showTitle) {
            {
                Text(
                    style = RuuviStationTheme.typography.dashboardValueTitle,
                    fontSize = titleFontSize,
                    text = stringResource(value.unitType.measurementName),
                )
            }
        } else {
            null
        },
    )
}

@Composable
fun AQIDisplay(
    value: AQI,
    alertTriggered: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (alertTriggered) {
        RuuviStationTheme.colors.activeAlertThemed
    } else {
        RuuviStationTheme.colors.dashboardValue
    }

    val bigValueFontSize = RuuviStationTheme.fontSizes.huge.limitScaleTo(1.5f)
    val unitFontSize = RuuviStationTheme.fontSizes.compact.limitScaleTo(1.2f)
    val titleFontSize = RuuviStationTheme.fontSizes.petite.limitScaleTo(1.2f)

    BigValueLayout(
        bigValueFontSize = bigValueFontSize,
        unitFontSize = unitFontSize,
        modifier = modifier,
        bigValue = {
            Text(
                style = RuuviStationTheme.typography.dashboardBigValue,
                text = value.scoreString,
                fontSize = bigValueFontSize,
                color = textColor,
            )
        },
        unit = {
            Text(
                style = RuuviStationTheme.typography.dashboardBigValueUnit,
                fontSize = unitFontSize,
                text = "/100",
                color = RuuviStationTheme.colors.dashboardValue,
            )
        },
        title = {
            Text(
                style = RuuviStationTheme.typography.dashboardValueTitle,
                fontSize = titleFontSize,
                text = stringResource(id = R.string.air_quality),
            )
        },
        footer = {
            GlowingProgressBarIndicator(
                progress = (value.score?.toFloat() ?: 0f) / 100F,
                lineColor = value.color,
            )
        },
    )
}

private enum class BigValueSlot { BigValue, Unit, Title, Footer }

// Glyph heights above the baseline as fractions of the font size, from the bundled fonts:
// Oswald Bold digits and Oswald Regular capitals.
private const val BIG_VALUE_DIGIT_TOP_EM = 0.82f
private const val UNIT_CAP_TOP_EM = 0.81f

/**
 * Big value with a unit as superscript and an optional measurement name as subscript, both
 * to the right of the value, and an optional [footer] spanning the full width underneath.
 *
 * The unit's cap height lines up with the top of the digits and the title sits on the
 * value's baseline, but never higher than the bottom of the unit's text box. These text
 * sizes scale differently (each has its own font scale limit, and Android 14+ scales small
 * text more than large text), so without that rule they can run into each other.
 */
@Composable
private fun BigValueLayout(
    bigValueFontSize: TextUnit,
    unitFontSize: TextUnit,
    bigValue: @Composable () -> Unit,
    unit: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    Layout(
        modifier = modifier,
        content = {
            Box(Modifier.layoutId(BigValueSlot.BigValue)) { bigValue() }
            Box(Modifier.layoutId(BigValueSlot.Unit)) { unit() }
            if (title != null) Box(Modifier.layoutId(BigValueSlot.Title)) { title() }
            if (footer != null) Box(Modifier.layoutId(BigValueSlot.Footer)) { footer() }
        },
    ) { measurables, constraints ->
        fun slot(id: BigValueSlot) = measurables.firstOrNull { it.layoutId == id }

        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val spacing = 4.dp.roundToPx()
        val bigPlaceable = slot(BigValueSlot.BigValue)!!.measure(loose)
        val sideX = bigPlaceable.width + spacing
        val sideConstraints = if (loose.hasBoundedWidth) {
            loose.copy(maxWidth = (loose.maxWidth - sideX).coerceAtLeast(0))
        } else {
            loose
        }
        val unitPlaceable = slot(BigValueSlot.Unit)!!.measure(sideConstraints)
        val titlePlaceable = slot(BigValueSlot.Title)?.measure(sideConstraints)

        val bigBaseline = bigPlaceable[FirstBaseline]
        val digitTop = bigBaseline - bigValueFontSize.toPx() * BIG_VALUE_DIGIT_TOP_EM
        val unitBaseline = digitTop + unitFontSize.toPx() * UNIT_CAP_TOP_EM
        val unitY = (unitBaseline - unitPlaceable[FirstBaseline]).roundToInt().coerceAtLeast(0)

        val titleY = titlePlaceable?.let {
            maxOf(bigBaseline - it[FirstBaseline], unitY + unitPlaceable.height)
        }

        val width = maxOf(
            bigPlaceable.width,
            sideX + maxOf(unitPlaceable.width, titlePlaceable?.width ?: 0),
        ).coerceIn(constraints.minWidth, constraints.maxWidth)
        val contentHeight = maxOf(
            bigPlaceable.height,
            unitY + unitPlaceable.height,
            titlePlaceable?.let { titleY!! + it.height } ?: 0,
        )
        val footerPlaceable = slot(BigValueSlot.Footer)
            ?.measure(Constraints.fixedWidth(width))
        val height = (contentHeight + (footerPlaceable?.height ?: 0))
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, height) {
            bigPlaceable.placeRelative(0, 0)
            unitPlaceable.placeRelative(sideX, unitY)
            titlePlaceable?.placeRelative(sideX, titleY!!)
            footerPlaceable?.placeRelative(0, contentHeight)
        }
    }
}

@Preview
@Composable
private fun BigValueDisplayPreview() {
    RuuviTheme {
        BigValueDisplay(
            value = EnvironmentValue(
                original = 22.50,
                value = 22.50,
                accuracy = Accuracy.Accuracy1,
                valueWithUnit = "22.5 %",
                valueWithoutUnit = "22.5",
                unitString = "%",
                unitType = UnitType.HumidityUnit.Relative
            ),
            alertTriggered = false,
            modifier = Modifier
        )
    }
}

@Preview
@Composable
private fun BigValueExtDisplayPreview() {
    RuuviTheme {
        BigValueExtDisplay(
            value = EnvironmentValue(
                original = 22.50,
                value = 22.50,
                accuracy = Accuracy.Accuracy1,
                valueWithUnit = "22.5 %",
                valueWithoutUnit = "22.5",
                unitString = "%",
                unitType = UnitType.HumidityUnit.Relative
            ),
            alertTriggered = false,
            showTitle = true,
            modifier = Modifier
        )
    }
}

@Preview
@Composable
private fun AQIDisplayDisplay() {
    RuuviTheme {
        AQIDisplay(
            value = AQI.getAQI(
                pm25 = 12.0,
                co2 = 11
            ),
            alertTriggered = false,
            modifier = Modifier
        )
    }
}
