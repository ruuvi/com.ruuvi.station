package com.ruuvi.station.dashboard.ui.dashboard_elements

import com.ruuvi.station.units.model.mouldRiskPresentationOrNull
import com.ruuvi.station.app.ui.components.indexSemantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.ruuvi.station.units.model.IndexPresentation
import com.ruuvi.station.units.model.toIndexPresentation
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Top
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.limitScaleTo
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType

@Composable
fun BigValueDisplay(
    value: EnvironmentValue,
    alertTriggered: Boolean,
    modifier: Modifier = Modifier
) {
    val index = value.mouldRiskPresentationOrNull()
    val textColor = if (alertTriggered) {
        RuuviStationTheme.colors.activeAlertThemed
    } else {
        index?.color ?: RuuviStationTheme.colors.settingsTitleText
    }

    Row(
        modifier = modifier.indexSemantics(index).wrapContentSize(),
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
            color = textColor
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
    val index = value.mouldRiskPresentationOrNull()
    val textColor = if (alertTriggered) {
        RuuviStationTheme.colors.activeAlertThemed
    } else {
        index?.color ?: RuuviStationTheme.colors.dashboardValue
    }

    ConstraintLayout (
        modifier = modifier.indexSemantics(index).wrapContentWidth()
    ){
        val (bigValue, subscript, superscript) = createRefs()

        Text(
            style = RuuviStationTheme.typography.dashboardBigValue,
            text = value.valueWithoutUnit,
            fontSize = RuuviStationTheme.fontSizes.huge.limitScaleTo(1.5f),
            color = textColor,
            modifier = Modifier.constrainAs(bigValue) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
            }
        )

        Text(
            style = RuuviStationTheme.typography.dashboardBigValueUnit,
            fontSize = RuuviStationTheme.fontSizes.compact.limitScaleTo(1.1f),
            text = value.unitString,
            modifier = Modifier
                .constrainAs(superscript) {
                    top.linkTo(bigValue.top, 7.dp)
                    start.linkTo(bigValue.end, 4.dp)
                }
        )

        if (showTitle) {
            Text(
                style = RuuviStationTheme.typography.dashboardValueTitle,
                fontSize = RuuviStationTheme.fontSizes.petite.limitScaleTo(1.1f),
                text = stringResource(value.unitType.measurementName),
                modifier = Modifier.constrainAs(subscript) {
                    start.linkTo(bigValue.end, 4.dp)
                    baseline.linkTo(bigValue.baseline)
                }
            )
        }
    }
}

@Composable
fun AQIDisplay(value: AQI, alertTriggered: Boolean, modifier: Modifier = Modifier) {
    IndexDisplay(value.toIndexPresentation(), alertTriggered, modifier)
}

@Composable
fun IndexDisplay(
    value: IndexPresentation,
    alertTriggered: Boolean,
    modifier: Modifier = Modifier
) {
    val accessibility = stringResource(R.string.index_accessibility, stringResource(value.title),
        value.scoreString, value.maximum, stringResource(value.description))
    val textColor = if (value.score == null) Color.Gray else if (alertTriggered) {
        RuuviStationTheme.colors.activeAlertThemed
    } else {
        RuuviStationTheme.colors.dashboardValue
    }

    ConstraintLayout (
        modifier = modifier.wrapContentWidth().clearAndSetSemantics { contentDescription = accessibility }
    ){
        val (bigValue, subscript, superscript, progress) = createRefs()

        Text(
            style = RuuviStationTheme.typography.dashboardBigValue,
            text = value.scoreString,
            fontSize = RuuviStationTheme.fontSizes.huge.limitScaleTo(1.5f),
            color = textColor,
            modifier = Modifier.constrainAs(bigValue) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
            }
        )

        Text(
            style = RuuviStationTheme.typography.dashboardBigValueUnit,
            fontSize = RuuviStationTheme.fontSizes.compact.limitScaleTo(1.2f),
            text = "/${value.maximum}",
            color = if (value.score == null) Color.Gray else Color.Unspecified,
            modifier = Modifier
                .constrainAs(superscript) {
                    top.linkTo(bigValue.top, 7.dp)
                    start.linkTo(bigValue.end, 4.dp)
                }
        )

        Text(
            style = RuuviStationTheme.typography.dashboardValueTitle,
            fontSize = RuuviStationTheme.fontSizes.petite.limitScaleTo(1.2f),
            text = stringResource(value.title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.constrainAs(subscript) {
                start.linkTo(bigValue.end, 4.dp)
                end.linkTo(parent.end)
                width = Dimension.preferredWrapContent
                baseline.linkTo(bigValue.baseline)
            }
        )

        GlowingProgressBarIndicator(
            progress = (value.score?.toFloat() ?: 0f) / value.maximum.toFloat(),
            lineColor = value.color,
            modifier = Modifier
                .constrainAs(progress) {
                    top.linkTo(bigValue.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
        )
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
