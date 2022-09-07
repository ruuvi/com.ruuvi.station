package com.ruuvi.station.alarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmElement
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun AlarmItems(viewModel: AlarmItemsViewModel) {
    Column {
        val alarmElements = viewModel.getAlarmsForSensor()
        if (alarmElements.isNotEmpty()) SensorSettingsTitle(title = stringResource(id = R.string.alerts))
        for (alarmElement in alarmElements.sortedBy { it.type.value }) {
            AlertEditItem(viewModel, alarmElement)
        }
    }
}

@Composable
fun SensorSettingsTitle (title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RuuviStationTheme.dimensions.sensorSettingTitleHeight)
            .background(color = RuuviStationTheme.colors.settingsTitle),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = RuuviStationTheme.typography.title,
            modifier = Modifier.padding(RuuviStationTheme.dimensions.medium)
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AlertEditItem(viewModel: AlarmItemsViewModel, alarmElement: AlarmElement) {
    var range by remember {
        mutableStateOf(alarmElement.low .. alarmElement.high)
    }

    ExpandableContainer(alarmElement.type.name) {
        var checked by remember { mutableStateOf(alarmElement.isEnabled) }
        SwitchRuuvi(
            text = "Alert",
            checked = checked,
            onCheckedChange = { checked = !checked },
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.medium)
        )
        DividerRuuvi()
        TextEditButton(
            value = null,
            emptyText = stringResource(id = R.string.alarm_custom_title_hint)
        ) { }
        DividerRuuvi()
        TextEditButton(
            value = stringResource(id = R.string.alert_subtitle_on, range.start, range.endInclusive),
            emptyText = ""
        ) { }

        RuuviRangeSlider(
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended),
            values = range,
            valueRange = alarmElement.type.range.first.toFloat() .. alarmElement.type.range.last.toFloat(),
            onValueChange = {
                range = it
                alarmElement.low = range.first
                alarmElement.high = range.last
            }
        )
    }
    DividerSurfaceColor()
}