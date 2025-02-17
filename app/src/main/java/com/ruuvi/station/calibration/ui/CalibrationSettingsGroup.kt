package com.ruuvi.station.calibration.ui

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.DividerRuuvi
import com.ruuvi.station.app.ui.components.ExpandableContainer
import com.ruuvi.station.app.ui.components.TextEditWithCaptionButton
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.calibration.model.CalibrationType
import com.ruuvi.station.tag.domain.RuuviTag

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CalibrationSettingsGroup(
    sensorState: RuuviTag,
    getTemperatureOffsetString: (Double) -> String,
    getHumidityOffsetString: (Double) -> String,
    getPressureOffsetString: (Double) -> String
) {
    val context = LocalContext.current

    ExpandableContainer(
        header = {
            Text(
                text = stringResource(id = R.string.offset_correction),
                style = RuuviStationTheme.typography.title,
            )
        },
        backgroundColor = RuuviStationTheme.colors.settingsTitle
    ) {
        if (sensorState.latestMeasurement?.temperature != null) {
            CalibrationItem(
                title = stringResource(id = R.string.temperature),
                value = getTemperatureOffsetString(sensorState.temperatureOffset ?: 0.0)
            ) {
                CalibrationActivity.start(context, sensorState.id, CalibrationType.TEMPERATURE)
            }
        }
        if (sensorState.latestMeasurement?.humidity != null) {
            DividerRuuvi()
            CalibrationItem(
                title = stringResource(id = R.string.humidity),
                value = getHumidityOffsetString(sensorState.humidityOffset ?: 0.0)
            ) {
                CalibrationActivity.start(context, sensorState.id, CalibrationType.HUMIDITY)
            }
        }
        if (sensorState.latestMeasurement?.pressure != null) {
            DividerRuuvi()
            CalibrationItem(
                title = stringResource(id = R.string.pressure),
                value = getPressureOffsetString(sensorState.pressureOffset ?: 0.0)
            ) {
                CalibrationActivity.start(context, sensorState.id, CalibrationType.PRESSURE)
            }
        }
    }
}

@Composable
fun CalibrationItem (
    title: String,
    value: String,
    action: () -> Unit
) {
    TextEditWithCaptionButton(
        title = title,
        value = value,
        icon = painterResource(id = R.drawable.arrow_forward_16),
        tint = RuuviStationTheme.colors.trackInactive
    ) {
        action.invoke()
    }
}