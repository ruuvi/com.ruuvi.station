package com.ruuvi.station.alarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmItemState
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun AlarmItems(viewModel: AlarmItemsViewModel) {
    val alarms = viewModel.alarms

    Column {
        if (alarms.isNotEmpty()) SensorSettingsTitle(title = stringResource(id = R.string.alerts))
        for (itemState in alarms.sortedBy { it.type.value }) {
            val title = viewModel.getTitle(itemState.type)
            if (itemState.type == AlarmType.MOVEMENT) {
                MovementAlertEditItem(
                    title = title,
                    alarmState = itemState,
                    changeEnabled = viewModel::setEnabled,
                    setDescription = viewModel::setDescription,
                )
            } else {
                AlertEditItem(
                    title = title,
                    alarmState = itemState,
                    changeEnabled = viewModel::setEnabled,
                    setDescription = viewModel::setDescription,
                    setRange = viewModel::setRange,
                    saveRange = viewModel::saveRange,
                    getPossibleRange = viewModel::getPossibleRange
                )
            }
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
fun AlertEditItem(
    title: String,
    alarmState: AlarmItemState,
    changeEnabled: (AlarmType, Boolean) -> Unit,
    setDescription: (AlarmType, String) -> Unit,
    setRange: (AlarmType, ClosedFloatingPointRange<Float>) -> Unit,
    saveRange: (AlarmType) -> Unit,
    getPossibleRange: (AlarmType) -> ClosedFloatingPointRange<Float>
) {
    var openDialog by remember { mutableStateOf(false) }
    val possibleRange by remember {
        mutableStateOf(getPossibleRange.invoke(alarmState.type))
    }

    ExpandableContainer(title) {
        SwitchRuuvi(
            text = "Alert",
            checked = alarmState.isEnabled,
            onCheckedChange = {
                changeEnabled.invoke(alarmState.type, it)
            },
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.medium)
        )
        DividerRuuvi()
        TextEditButton(
            value = alarmState.customDescription,
            emptyText = stringResource(id = R.string.alarm_custom_title_hint)
        ) {
            openDialog = true
        }
        DividerRuuvi()
        TextEditButton(
            value = stringResource(
                id = R.string.alert_subtitle_on,
                alarmState.displayLow,
                alarmState.displayHigh),
            emptyText = ""
        ) { }

        RuuviRangeSlider(
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended),
            values = alarmState.rangeLow .. alarmState.rangeHigh,
            valueRange = possibleRange,
            onValueChange = {
                setRange.invoke(alarmState.type, it)
            },
            onValueChangeFinished = {
                saveRange.invoke(alarmState.type)
            }
        )
    }
    DividerSurfaceColor()

    if (openDialog) {
        ChangeDescriptionDialog(
            alarmState = alarmState,
            setDescription = setDescription
        ) {
            openDialog = false
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MovementAlertEditItem(
    title: String,
    alarmState: AlarmItemState,
    changeEnabled: (AlarmType, Boolean) -> Unit,
    setDescription: (AlarmType, String) -> Unit,
) {
    var openDescriptionDialog by remember { mutableStateOf(false) }

    ExpandableContainer(title) {
        SwitchRuuvi(
            text = "Alert",
            checked = alarmState.isEnabled,
            onCheckedChange = {
                changeEnabled.invoke(alarmState.type, it)
            },
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.medium)
        )
        DividerRuuvi()
        TextEditButton(
            value = alarmState.customDescription,
            emptyText = stringResource(id = R.string.alarm_custom_title_hint)
        ) {
            openDescriptionDialog = true
        }
        DividerRuuvi()
        Row(
            modifier = Modifier
                .height(RuuviStationTheme.dimensions.sensorSettingTitleHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RuuviStationTheme.dimensions.medium),
                style = RuuviStationTheme.typography.paragraph,
                textAlign = TextAlign.End,
                text = stringResource(id = R.string.alert_movement_description))
        }
    }
    DividerSurfaceColor()

    if (openDescriptionDialog) {
        ChangeDescriptionDialog(
            alarmState = alarmState,
            setDescription = setDescription
        ) {
            openDescriptionDialog = false
        }
    }
}

@Composable
fun ChangeDescriptionDialog(
    alarmState: AlarmItemState,
    setDescription: (AlarmType, String) -> Unit,
    onDismissRequest : () -> Unit
) {
    var description by remember {
        mutableStateOf(alarmState.customDescription)
    }
    RuuviDialog(
        title = stringResource(id = R.string.alarm_custom_description_title),
        onDismissRequest = onDismissRequest,
        onClickAction = {
            setDescription.invoke(alarmState.type, description)
        }
    ) {
        TextFieldRuuvi(
            value = description,
            onValueChange = {
                description = it
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun EditAlertDialog(
) {

}