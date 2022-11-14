package com.ruuvi.station.alarm.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmItemState
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.tagsettings.ui.SensorSettingsTitle
import kotlinx.coroutines.delay
import timber.log.Timber
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AlarmsGroup(viewModel: AlarmItemsViewModel) {
    val notificationPermissionState = rememberPermissionState(
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    var permissionAsked by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = true) {
        Timber.d("Alarms LaunchedEffect")
        viewModel.initAlarms()
        while (true) {
            Timber.d("AlarmItems refreshAlarmState ")
            viewModel.refreshAlarmState()
            delay(1000)
        }
    }
    Timber.d("AlarmItems refresh ")
    val alarms = viewModel.alarms

    if (!notificationPermissionState.status.isGranted && !permissionAsked && alarms.any { it.isEnabled }) {
        permissionAsked = true
        LaunchedEffect(key1 = true) {
            notificationPermissionState.launchPermissionRequest()
        }
    }

    Column {
        if (alarms.isNotEmpty()) SensorSettingsTitle(title = stringResource(id = R.string.alerts))
        for (itemState in alarms.sortedBy { it.type.value }) {
            val title = viewModel.getTitle(itemState.type)
            when (itemState.type) {
                AlarmType.TEMPERATURE, AlarmType.HUMIDITY, AlarmType.PRESSURE ->
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
                AlarmType.RSSI ->
                    RssiAlertEditItem(
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
                AlarmType.MOVEMENT ->
                    MovementAlertEditItem(
                        title = title,
                        alarmState = itemState,
                        changeEnabled = viewModel::setEnabled,
                        setDescription = viewModel::setDescription,
                    )
            }
        }
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
        SwitchIndicatorRuuvi(
            text = getMutedText(LocalContext.current, alarmState.mutedTill),
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RssiAlertEditItem(
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
        SmallerParagraph(
            modifier = Modifier.padding(RuuviStationTheme.dimensions.medium),
            text = stringResource(id = R.string.rssi_alert_description)
        )
        SwitchIndicatorRuuvi(
            text = getMutedText(LocalContext.current, alarmState.mutedTill),
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
        SwitchIndicatorRuuvi(
            text = getMutedText(LocalContext.current, alarmState.mutedTill),
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
        val focusManager = LocalFocusManager.current

        TextFieldRuuvi(
            value = description,
            onValueChange = {
                if (it.length <= 32) description = it
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            keyboardActions = KeyboardActions(onDone = {focusManager.clearFocus()})
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
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
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current

        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))

        Subtitle(text = stringResource(id = R.string.alert_dialog_min, possibleRange.start.toInt().toString()))
        NumberTextFieldRuuvi(
            value = alarmState.displayLow,
            keyboardActions = KeyboardActions(onDone = {focusRequester.requestFocus()})
        ) { parsed, value ->
            min = value
        }

        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

        Subtitle(text = stringResource(id = R.string.alert_dialog_max, possibleRange.endInclusive.toInt().toString()))
        NumberTextFieldRuuvi(
            value = alarmState.displayHigh,
            modifier = Modifier.focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
            })
        ) { parsed, value ->
            max = value
        }
    }
}

fun getMutedText(context: Context, mutedTill: Date?): String {
    return if (mutedTill != null && mutedTill > Date()) {
        context.getString(R.string.muted_till, DateFormat.getTimeInstance(DateFormat.SHORT).format(mutedTill))
    } else {
        ""
    }
}