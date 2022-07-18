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
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.TEMPERATURE)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_background_scan),
                description = intervalText,
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.TEMPERATURE)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_temperature_unit),
                description = stringResource(id = viewModel.getTemperatureUnit().unit),
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.TEMPERATURE)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_pressure_unit),
                description = null,
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.TEMPERATURE)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_humidity_unit),
                description = null,
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.TEMPERATURE)) }
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
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .padding(RuuviStationTheme.dimensions.medium)
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                style = RuuviStationTheme.typography.subtitle,
                text = name,
                textAlign = TextAlign.Left
            )
            if (description != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Paragraph(text = description)
                    Text(text = "", )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.arrow_forward_16),
                    contentDescription = "")
            }
        }
    }

}