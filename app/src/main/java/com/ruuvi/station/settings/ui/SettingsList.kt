package com.ruuvi.station.settings.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.components.DividerRuuvi
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.util.BackgroundScanModes

@Composable
fun SettingsList(
    scaffoldState: ScaffoldState,
    onNavigate: (UiEvent.Navigate) -> Unit,
    viewModel: AppSettingsListViewModel
) {
    var intervalText = ""
    if (viewModel.getBackgroundScanMode() != BackgroundScanModes.DISABLED) {
        val bgScanInterval = viewModel.getBackgroundScanInterval()
        val min = bgScanInterval / 60
        val sec = bgScanInterval - min * 60
        if (min > 0) intervalText += min.toString() + " " + stringResource(R.string.min) + " "
        if (sec > 0) intervalText += sec.toString() + " " + stringResource(R.string.sec)
    } else {
        intervalText = stringResource(id = R.string.alert_subtitle_off)
    }

    LazyColumn() {
        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_appearance),
                description = null,
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.APPEARANCE)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_background_scan),
                description = intervalText,
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.BACKGROUNDSCAN)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_temperature_unit),
                description = stringResource(id = viewModel.getTemperatureUnit().title),
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.TEMPERATURE)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_humidity_unit),
                description = stringResource(id = viewModel.getHumidityUnit().title),
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.HUMIDITY)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_pressure_unit),
                description = stringResource(id = viewModel.getPressureUnit().title),
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.PRESSURE)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_chart),
                description = null,
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.CHARTS)) }
            )
        }

    }
}

@Composable
fun SettingsElement(
    name: String,
    description: String?,
    onClick: () -> Unit
) {
    Column() {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(RuuviStationTheme.dimensions.medium)
                .height(RuuviStationTheme.dimensions.settingsListHeight)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .fillMaxWidth(),
                style = RuuviStationTheme.typography.subtitle,
                text = name,
                textAlign = TextAlign.Left
            )


            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {

                if (description != null) {
                    Paragraph(
                        text = description,
                        modifier = Modifier.padding(end = RuuviStationTheme.dimensions.extended)
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.arrow_forward_16),
                    contentDescription = ""
                )
            }
        }
        DividerRuuvi()
    }

}