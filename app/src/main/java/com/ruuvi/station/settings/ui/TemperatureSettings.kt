package com.ruuvi.station.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.RadioButton
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.PageSurfaceWithPadding
import com.ruuvi.station.app.ui.components.SubtitleWithPadding
import com.ruuvi.station.app.ui.components.ruuviRadioButtonColors
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.units.model.TemperatureUnit

@Composable
fun TemperatureSettings(
    scaffoldState: ScaffoldState
) {
    PageSurfaceWithPadding {
        Column() {
            TemperatureUnit()
            TemperatureAccuracy()
        }
    }
}

@Composable
fun TemperatureUnit() {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubtitleWithPadding(text = stringResource(id = R.string.settings_temperature_unit))

        for (item in TemperatureUnit.values()) {
            TemperatureUnitElement(temperatureUnit = item, false)
        }
    }
}

@Composable
fun TemperatureUnitElement(
    temperatureUnit: TemperatureUnit,
    isSelected: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable {  }
    ) {
        RadioButton(
            selected = (isSelected),
            colors = ruuviRadioButtonColors(),
            onClick = {  })

        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(id = temperatureUnit.title),
            style = RuuviStationTheme.typography.paragraph)
    }
}

@Composable
fun TemperatureAccuracy() {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubtitleWithPadding(text = stringResource(id = R.string.settings_temperature_unit))

        for (item in TemperatureUnit.values()) {
            TemperatureUnitElement(temperatureUnit = item, false)
        }
    }
}


