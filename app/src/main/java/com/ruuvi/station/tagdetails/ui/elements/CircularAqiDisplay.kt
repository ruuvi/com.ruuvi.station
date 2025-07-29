package com.ruuvi.station.tagdetails.ui.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.CircularGradientProgress
import com.ruuvi.station.app.ui.components.scaleUpTo
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.EnvironmentValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircularAQIDisplay(
    value: EnvironmentValue,
    aqi: AQI,
    alertActive: Boolean,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    var progressSize = 130.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularGradientProgress(
            progress = aqi.score?.toFloat() ?: 0f,
            progressText = aqi.scoreString,
            lineColor = aqi.color,
            size = progressSize
        )
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
        Text(
            text = stringResource(aqi.descriptionRes),
            style = RuuviStationTheme.typography.dashboardValue,
            fontSize = 18.sp,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

        SensorUnitName(
            icon = R.drawable.icon_air_quality,
            name = stringResource(R.string.air_quality),
            itemHeight = RuuviStationTheme.dimensions.sensorCardValueItemHeight.scaleUpTo(1.5f),
            alertActive = alertActive,
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended)
        ) {
            showBottomSheet = true
        }
    }

    if (showBottomSheet) {
        ValueBottomSheet(
            sheetValue = value,
            chartHistory = null,
            modifier = Modifier
        ) {
            showBottomSheet = false
        }
    }
}

val environmentValue by lazy {
    EnvironmentValue(
        original = TODO(),
        value = TODO(),
        accuracy = TODO(),
        valueWithUnit = TODO(),
        valueWithoutUnit = TODO(),
        unitString = TODO(),
        unitType = TODO()
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
                co2 = null,
                nox = null,
                voc = null
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
                co2 = 11,
                nox = 10,
                voc = 10
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
                co2 = 12,
                nox = 15,
                voc = 15
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
                co2 = 11,
                nox = 1,
                voc = 1
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
                co2 = 11,
                nox = 1,
                voc = 1
            ),
            alertActive = false
        )
    }
}