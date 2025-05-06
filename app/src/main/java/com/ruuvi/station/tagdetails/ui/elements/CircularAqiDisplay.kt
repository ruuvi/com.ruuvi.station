package com.ruuvi.station.tagdetails.ui.elements

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
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.CircularGradientProgress
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.util.extensions.scaledSp

@Composable
fun CircularAQIDisplay(
    aqi: AQI,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularGradientProgress(
            progress = aqi.score?.toFloat() ?: 0f,
            lineColor = aqi.color
        )
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
        Text(
            text = stringResource(aqi.descriptionRes),
            style = RuuviStationTheme.typography.dashboardValue,
            fontSize = 20.scaledSp,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))

        SensorValueName(
            icon = R.drawable.icon_air_quality,
            name = stringResource(R.string.air_quality),
            itemHeight = RuuviStationTheme.dimensions.sensorCardValueItemHeight,
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended)
        )
    }
}

@Preview
@Composable
private fun CircularAQIDisplayPreviewNull() {
    RuuviTheme {
        CircularAQIDisplay(AQI.getAQI(
            pm25 = null,
            co2 = null,
            nox = null,
            voc = null
        )
        )
    }
}

@Preview
@Composable
private fun CircularAQIDisplayPreviewUnhealthy() {
    RuuviTheme {
        CircularAQIDisplay(AQI.getAQI(
            pm25 = 95.0,
            co2 = 11,
            nox = 10,
            voc = 10
        )
        )
    }
}

@Preview
@Composable
private fun CircularAQIDisplayPreviewAverage() {
    RuuviTheme {
        CircularAQIDisplay(AQI.getAQI(
            pm25 = 77.0,
            co2 = 12,
            nox = 15,
            voc = 15
        )
        )
    }
}

@Preview
@Composable
private fun CircularAQIDisplayPreviewGood() {
    RuuviTheme {
        CircularAQIDisplay(AQI.getAQI(
            pm25 = 50.0,
            co2 = 11,
            nox = 1,
            voc = 1
        )
        )
    }
}


@Preview
@Composable
private fun CircularAQIDisplayPreview() {
    RuuviTheme {
        CircularAQIDisplay(AQI.getAQI(
            pm25 = 12.0,
            co2 = 11,
            nox = 1,
            voc = 1
        )
        )
    }
}