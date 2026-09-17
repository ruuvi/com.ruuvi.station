package com.ruuvi.station.tagdetails.ui.elements

import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import com.ruuvi.station.units.model.IndexPresentation
import com.ruuvi.station.units.model.toIndexPresentation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.CircularGradientProgress
import com.ruuvi.station.app.ui.components.scaleUpTo
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType

@Composable
fun CircularAQIDisplay(
    value: EnvironmentValue,
    aqi: AQI,
    alertActive: Boolean,
    modifier: Modifier = Modifier,
    clickAction: () -> Unit = {}
) {
    CircularIndexDisplay(aqi.toIndexPresentation(), alertActive, modifier, clickAction)
}

@Composable
fun CircularIndexDisplay(
    index: IndexPresentation,
    alertActive: Boolean,
    modifier: Modifier = Modifier,
    clickAction: () -> Unit = {}
) {

    val accessibility = stringResource(R.string.index_accessibility, stringResource(index.title),
        index.scoreString, index.maximum, stringResource(index.description))
    Column(
        modifier = modifier.clickable(onClick = clickAction).clearAndSetSemantics {
            contentDescription = accessibility
            onClick { clickAction(); true }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        CircularGradientProgress(
            progress = (index.score?.toFloat() ?: 0f) * 100f / index.maximum,
            progressText = index.scoreString,
            lineColor = index.color,
            maximum = index.maximum,
            textColor = if (index.score == null) Color.Gray else Color.White
        )
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
        Text(
            text = stringResource(index.description),
            style = RuuviStationTheme.typography.dashboardValue,
            fontSize = 18.sp,
            color = if (index.score == null) Color.Gray else Color.White
        )
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

        SensorUnitName(
            icon = index.icon,
            name = stringResource(index.title),
            itemHeight = RuuviStationTheme.dimensions.sensorCardValueItemHeight.scaleUpTo(1.5f),
            alertActive = alertActive,
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended)
        ) {
            clickAction.invoke()
        }
    }
}

val environmentValue by lazy {
    EnvironmentValue(
        original = 0.0,
        value = 0.0,
        accuracy = Accuracy.Accuracy0,
        valueWithUnit = "0",
        valueWithoutUnit = "0",
        unitString = "AQI",
        unitType = UnitType.AirQuality.AqiIndex
    )
}

@Preview
@Composable
private fun CircularAQIDisplayPreviewNull() {
    RuuviTheme {
        CircularAQIDisplay(
            environmentValue,
            AQI.getAQI(
                pm25 = null,
                co2 = null
            ),
            alertActive = false
        )
    }
}

@Preview
@Composable
private fun CircularAQIDisplayPreviewUnhealthy() {
    RuuviTheme {
        CircularAQIDisplay(
            environmentValue,
            AQI.getAQI(
                pm25 = 95.0,
                co2 = 11
            ),
            alertActive = false
        )
    }
}

@Preview
@Composable
private fun CircularAQIDisplayPreviewAverage() {
    RuuviTheme {
        CircularAQIDisplay(
            environmentValue,
            AQI.getAQI(
                pm25 = 77.0,
                co2 = 12
            ),
            alertActive = false
        )
    }
}

@Preview
@Composable
private fun CircularAQIDisplayPreviewGood() {
    RuuviTheme {
        CircularAQIDisplay(
            environmentValue,
            AQI.getAQI(
                pm25 = 50.0,
                co2 = 11
            ),
            alertActive = false
        )
    }
}


@Preview
@Composable
private fun CircularAQIDisplayPreview() {
    RuuviTheme {
        CircularAQIDisplay(
            environmentValue,
            AQI.getAQI(
                pm25 = 12.0,
                co2 = 11
            ),
            alertActive = false
        )
    }
}