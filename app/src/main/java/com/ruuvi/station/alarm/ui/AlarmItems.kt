package com.ruuvi.station.alarm.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmItemState
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun AlarmItems(viewModel: AlarmItemsViewModel) {
    val alarms = viewModel.alarms

    Timber.d("AlarmItems refresh ")
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
                    getPossibleRange = viewModel::getPossibleRange,
                    validateRange = viewModel::validateRange,
                    manualRangeSave = viewModel::manualRangeSave
                )
            }
        }
    }

    LaunchedEffect(key1 = 2) {
        while (true) {
            Timber.d("AlarmItems refreshAlarmState ")
            viewModel.refreshAlarmState()
            delay(3000)
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
    getPossibleRange: (AlarmType) -> ClosedFloatingPointRange<Float>,
    validateRange: (AlarmType, Double?, Double?) -> Boolean,
    manualRangeSave: (AlarmType, Double?, Double?) -> Unit,
) {
    var openDescriptionDialog by remember { mutableStateOf(false) }
    var openAlarmEditDialog by remember { mutableStateOf(false) }
    val possibleRange by remember {
        mutableStateOf(getPossibleRange.invoke(alarmState.type))
    }

    ExpandableContainer(header = {
        AlarmHeader(title, alarmState)
    }) {
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
        TextEditButton(
            value = stringResource(
                id = R.string.alert_subtitle_on,
                alarmState.displayLow,
                alarmState.displayHigh),
            emptyText = ""
        ) {
            openAlarmEditDialog = true
        }

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

    if (openDescriptionDialog) {
        ChangeDescriptionDialog(
            alarmState = alarmState,
            setDescription = setDescription
        ) {
            openDescriptionDialog = false
        }
    }

    if (openAlarmEditDialog) {
        AlarmEditDialog(
            alarmState = alarmState,
            getPossibleRange = getPossibleRange,
            validateRange = validateRange,
            manualRangeSave = manualRangeSave
        ) {
            openAlarmEditDialog = false
        }
    }
}

@Composable
private fun AlarmHeader(
    title: String,
    alarmState: AlarmItemState
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Subtitle(
            text = title,
            modifier = Modifier.weight(1f)
        )
        if (alarmState.isEnabled) {
            if (alarmState.triggered) {
                var imageVisible by remember { mutableStateOf(true) }

                AnimatedVisibility(
                    visible = imageVisible,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_notifications_active_24px),
                        contentDescription = null,
                        tint = RuuviStationTheme.colors.activeAlert
                    )
                }
                LaunchedEffect(key1 = 1) {
                    while (true) {
                        delay(800)
                        imageVisible = !imageVisible
                    }
                }
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_notifications_on_24px),
                    contentDescription = null,
                    tint = RuuviStationTheme.colors.accent
                )
            }
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

    ExpandableContainer(header = {
        AlarmHeader(title, alarmState)
    }) {
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
        onOkClickAction = {
            setDescription.invoke(alarmState.type, description)
            onDismissRequest.invoke()
        }
    ) {
        TextFieldRuuvi(
            value = description,
            onValueChange = {
                description = it
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
    }
}

@Composable
fun AlarmEditDialog(
    alarmState: AlarmItemState,
    getPossibleRange: (AlarmType) -> ClosedFloatingPointRange<Float>,
    validateRange: (AlarmType, Double?, Double?) -> Boolean,
    manualRangeSave: (AlarmType, Double?, Double?) -> Unit,
    onDismissRequest : () -> Unit
) {
    val title = when (alarmState.type) {
        AlarmType.TEMPERATURE -> stringResource(id = R.string.alert_dialog_title_temperature)
        AlarmType.PRESSURE -> stringResource(id = R.string.alert_dialog_title_pressure)
        AlarmType.HUMIDITY -> stringResource(id = R.string.alert_dialog_title_humidity)
        AlarmType.RSSI -> stringResource(id = R.string.alert_dialog_title_rssi)
        else -> ""
    }

    val possibleRange by remember {
        mutableStateOf(getPossibleRange.invoke(alarmState.type))
    }

    var min by remember {
        mutableStateOf<Double?>(alarmState.rangeLow.toDouble())
    }

    var max by remember {
        mutableStateOf<Double?>(alarmState.rangeHigh.toDouble())
    }

    val focusRequester = remember { FocusRequester() }

    RuuviDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        onOkClickAction = {
            manualRangeSave.invoke(alarmState.type, min, max)
            onDismissRequest.invoke()
        },
        validation = {
            validateRange(alarmState.type, min, max)
        }
    ) {
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))

        Subtitle(text = stringResource(id = R.string.alert_dialog_min, possibleRange.start.toInt().toString()))
        NumberTextFieldRuuvi(
            value = min.toString(),
            keyboardActions = KeyboardActions(onDone = {focusRequester.requestFocus()})
        ) { parsed, value ->
            min = value
        }

        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

        Subtitle(text = stringResource(id = R.string.alert_dialog_max, possibleRange.endInclusive.toInt().toString()))
        NumberTextFieldRuuvi(
            value = max.toString(),
            modifier = Modifier.focusRequester(focusRequester),
        ) { parsed, value ->
            max = value
        }
    }
}