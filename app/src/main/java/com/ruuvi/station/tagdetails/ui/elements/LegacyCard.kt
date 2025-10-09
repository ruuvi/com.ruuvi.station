package com.ruuvi.station.tagdetails.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.White80
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.isAir
import com.ruuvi.station.units.model.UnitType

@Composable
fun SensorCardLegacy(
    modifier: Modifier = Modifier,
    sensor: RuuviTag
) {

    val valuesWithoutFirst = if (sensor.valuesToDisplay.isNotEmpty()) {
        sensor.valuesToDisplay.subList(1, sensor.valuesToDisplay.size)
    } else {
        listOf()
    }

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val firstValue = sensor.valuesToDisplay.firstOrNull()
        if (firstValue != null) {
            if (firstValue.unitType is UnitType.AirQuality) {
                if (sensor.latestMeasurement != null) {
                    CircularAQIDisplay(
                        value = firstValue,
                        aqi = sensor.latestMeasurement.aqiScore,
                        alertActive = firstValue.unitType.alarmType?.let {
                            sensor.alarmSensorStatus.triggered(it)
                        } ?: false
                    )
                }
            } else {
                BigValueDisplay(
                    modifier = Modifier.padding(24.dp),
                    value = firstValue,
                    showName = false,
                    alertActive = firstValue.unitType.alarmType?.let {
                        sensor.alarmSensorStatus.triggered(it)
                    } ?: false
                )
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Bottom
        ) {
            SensorValuesLegacy(
                modifier = Modifier,
                sensor = sensor,
            )

        }
    }

}

@Composable
fun SensorValuesLegacy(
    modifier: Modifier,
    sensor: RuuviTag
) {
    if (sensor.isAir()) {
        Row (
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(start = RuuviStationTheme.dimensions.extended),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                sensor.latestMeasurement?.temperature?.let {
                    SensorValueItemLegacy(R.drawable.icon_measure_small_temp, it.valueWithoutUnit, it.unitString, stringResource(R.string.temperature))
                    Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
                }

                sensor.latestMeasurement?.pressure?.let {
                    SensorValueItemLegacy(R.drawable.icon_measure_pressure, it.valueWithoutUnit, it.unitString, stringResource(R.string.pressure))
                    Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
                }

                sensor.latestMeasurement?.co2?.let {
                    SensorValueItemLegacy(R.drawable.icon_measure_small_temp, it.valueWithoutUnit, it.unitString, stringResource(R.string.co2))
                    Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
                }

                sensor.latestMeasurement?.voltage?.let {
                    SensorValueItemLegacy(R.drawable.icon_measure_small_temp, it.valueWithoutUnit, it.unitString, stringResource(R.string.battery))
                    Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(start = RuuviStationTheme.dimensions.extended),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                sensor.latestMeasurement?.humidity?.let {
                    SensorValueItemLegacy(R.drawable.icon_measure_humidity, it.valueWithoutUnit, it.unitString, stringResource(R.string.humidity))
                    Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
                }

                sensor.latestMeasurement?.pm25?.let {
                    SensorValueItemLegacy(R.drawable.icon_measure_small_temp, it.valueWithoutUnit, it.unitString, stringResource(R.string.pm25))
                    Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
                }

                sensor.latestMeasurement?.nox?.let {
                    SensorValueItemLegacy(R.drawable.icon_measure_small_temp, it.valueWithoutUnit, "", stringResource(R.string.nox))
                    Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
                }

                sensor.latestMeasurement?.luminosity?.let {
                    SensorValueItemLegacy(R.drawable.icon_measure_small_temp, it.valueWithoutUnit, it.unitString,stringResource(R.string.light))
                    Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
                }

                sensor.latestMeasurement?.dBaAvg?.let {
                    SensorValueItemLegacy(R.drawable.icon_measure_small_temp, it.valueWithoutUnit, it.unitString, stringResource(R.string.sound))
                    Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .padding(start = RuuviStationTheme.dimensions.extended),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            sensor.latestMeasurement?.humidity?.let {
                SensorValueItemLegacy(R.drawable.icon_measure_humidity, it.valueWithoutUnit, it.unitString, stringResource(R.string.humidity))
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
            }

            sensor.latestMeasurement?.pressure?.let {
                SensorValueItemLegacy(R.drawable.icon_measure_pressure, it.valueWithoutUnit, it.unitString, stringResource(R.string.pressure))
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
            }

            sensor.latestMeasurement?.movement?.let {
                SensorValueItemLegacy(R.drawable.ic_icon_measure_movement, it.valueWithoutUnit, "", stringResource(R.string.movements))
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

            }
        }
    }
}

@Composable
fun SensorValueItemLegacy(
    icon: Int,
    value: String,
    unit: String,
    name: String = ""
) {
    Row (
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            painter = painterResource(id = icon),
            tint = Color.White,
            contentDescription = ""
        )
        Column {
            Row() {
                Text(
                    modifier = Modifier
                        .alignByBaseline()
                        .padding(
                            start = RuuviStationTheme.dimensions.extended
                        ),
                    fontSize = RuuviStationTheme.fontSizes.extended,
                    style = RuuviStationTheme.typography.dashboardBigValueUnit,
                    fontFamily = ruuviStationFonts.mulishBold,
                    fontWeight = FontWeight.Bold,
                    text = value,
                    color = Color.White
                )

                Text(
                    modifier = Modifier
                        .alignByBaseline()
                        .padding(
                            start = RuuviStationTheme.dimensions.small
                        ),
                    style = RuuviStationTheme.typography.dashboardSecondary,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = RuuviStationTheme.fontSizes.compact,
                    text = unit,
                )
            }
            Text(
                modifier = Modifier
                    .padding(
                        start = RuuviStationTheme.dimensions.extended
                    ),
                style = RuuviStationTheme.typography.dashboardSecondary,
                color = White80,
                fontSize = RuuviStationTheme.fontSizes.compact,
                text = name,
            )
        }
    }
}