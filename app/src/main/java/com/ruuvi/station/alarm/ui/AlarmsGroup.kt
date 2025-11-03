package com.ruuvi.station.alarm.ui

import android.content.Context
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmItemState
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.components.dialog.CustomContentDialog
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.bluetooth.util.extensions.roundHalfUp
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagsettings.ui.SensorSettingsTitle
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.UnitType
import kotlinx.coroutines.delay
import timber.log.Timber
import java.text.DateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AlarmsGroup(
    scaffoldState: ScaffoldState,
    viewModel: AlarmItemsViewModel
) {
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
            viewModel.refreshSensorState()
            delay(1000)
        }
    }
    Timber.d("AlarmItems refresh ")
    val alarms = viewModel.alarms

    val sensorState by viewModel.sensorState.collectAsState()

    if (!notificationPermissionState.status.isGranted && !permissionAsked && alarms.any { it.isEnabled.value }) {
        permissionAsked = true
        LaunchedEffect(key1 = true) {
            notificationPermissionState.launchPermissionRequest()
        }
    }

    Column {
        if (alarms.isNotEmpty()) SensorSettingsTitle(title = stringResource(id = R.string.alerts))
        for (itemState in alarms) {
            Timber.d("AlarmItem $itemState")
            val title = viewModel.getTitle(itemState.type)
            when (itemState.type) {
                AlarmType.TEMPERATURE, AlarmType.HUMIDITY, AlarmType.PRESSURE, AlarmType.CO2, AlarmType.PM25, AlarmType.PM10, AlarmType.PM40, AlarmType.PM100, AlarmType.VOC, AlarmType.NOX, AlarmType.SOUND, AlarmType.LUMINOSITY, AlarmType.AQI->
                    AlertEditItem(
                        title = title,
                        alarmState = itemState,
                        sensorState = sensorState,
                        unitsConverter = viewModel.unitsConverter,
                        changeEnabled = viewModel::setEnabled,
                        setDescription = viewModel::setDescription,
                        setRange = viewModel::setRange,
                        saveRange = viewModel::saveRange,
                        getPossibleRange = viewModel::getPossibleRange,
                        getExtraRange = viewModel::getExtraRange,
                        validateRange = viewModel::validateRange,
                        manualRangeSave = viewModel::manualRangeSave,
                        getUnit = viewModel::getUnit
                    )
                AlarmType.RSSI ->
                    RssiAlertEditItem(
                        title = title,
                        alarmState = itemState,
                        sensorState = sensorState,
                        changeEnabled = viewModel::setEnabled,
                        setDescription = viewModel::setDescription,
                        setRange = viewModel::setRange,
                        saveRange = viewModel::saveRange,
                        getPossibleRange = viewModel::getPossibleRange,
                        validateRange = viewModel::validateRange,
                        manualRangeSave = viewModel::manualRangeSave,
                        getUnit = viewModel::getUnit
                    )
                AlarmType.MOVEMENT ->
                    MovementAlertEditItem(
                        title = title,
                        alarmState = itemState,
                        changeEnabled = viewModel::setEnabled,
                        setDescription = viewModel::setDescription,
                    )
                AlarmType.OFFLINE ->
                    OfflineAlertEditItem(
                        title = title,
                        alarmState = itemState,
                        changeEnabled = viewModel::setEnabled,
                        setDescription = viewModel::setDescription,
                        manualRangeSave = viewModel::manualRangeSave
                    )
                else -> {}
            }
        }
    }

    ShowStatusSnackbar(
        scaffoldState = scaffoldState,
        uiEvent = viewModel.uiEvent
    )
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OfflineAlertEditItem(
    title: String,
    alarmState: AlarmItemState,
    changeEnabled: (AlarmType, Boolean) -> Unit,
    setDescription: (AlarmType, String) -> Unit,
    manualRangeSave: (AlarmType, Double?, Double?) -> Unit,
) {
    var openDescriptionDialog by remember { mutableStateOf(false) }
    var openAlarmEditDialog by remember { mutableStateOf(false) }


    ExpandableContainer(header = {
        AlarmHeader(title, alarmState)
    }) {
        SwitchIndicatorRuuvi(
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            text = getMutedText(LocalContext.current, alarmState.mutedTill),
            checked = alarmState.isEnabled.value,
            onCheckedChange = {
                changeEnabled.invoke(alarmState.type, it)
            },
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
                id = R.string.alert_cloud_connection_description,
                (alarmState.max / 60.0).roundToInt()),
            emptyText = "",
            applyBoldStyleToDecimals = true
        ) {
            openAlarmEditDialog = true
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

    if (openAlarmEditDialog) {
        OfflineAlarmEditDialog(
            alarmState = alarmState,
            manualRangeSave = manualRangeSave,
        ) {
            openAlarmEditDialog = false
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AlertEditItem(
    title: String,
    alarmState: AlarmItemState,
    sensorState: RuuviTag,
    unitsConverter: UnitsConverter,
    changeEnabled: (AlarmType, Boolean) -> Unit,
    setDescription: (AlarmType, String) -> Unit,
    setRange: (AlarmType, ClosedFloatingPointRange<Float>) -> Unit,
    saveRange: (AlarmType) -> Unit,
    getPossibleRange: (AlarmType) -> ClosedFloatingPointRange<Float>,
    getExtraRange: (AlarmType) -> ClosedFloatingPointRange<Float>,
    validateRange: (AlarmType, Double?, Double?) -> Boolean,
    manualRangeSave: (AlarmType, Double?, Double?) -> Unit,
    getUnit: (AlarmType) -> String
) {
    var openDescriptionDialog by remember { mutableStateOf(false) }
    var openAlarmEditDialog by remember { mutableStateOf(false) }
    val possibleRange = if (alarmState.extended) {
        getExtraRange.invoke(alarmState.type)
    } else {
        getPossibleRange.invoke(alarmState.type)
    }

    ExpandableContainer(header = {
        AlarmHeader(title, alarmState)
    }) {
        SwitchIndicatorRuuvi(
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            text = getMutedText(LocalContext.current, alarmState.mutedTill),
            checked = alarmState.isEnabled.value,
            onCheckedChange = {
                changeEnabled.invoke(alarmState.type, it)
            },
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
            emptyText = "",
            applyBoldStyleToDecimals = true
        ) {
            openAlarmEditDialog = true
        }

        RuuviRangeSlider(
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            values = alarmState.rangeLow .. alarmState.rangeHigh,
            valueRange = possibleRange,
            onValueChange = {
                setRange.invoke(alarmState.type, it)
            },
            onValueChangeFinished = {
                saveRange.invoke(alarmState.type)
            }
        )

        if (sensorState.latestMeasurement != null) {
            val latestValue = when (alarmState.type) {
                AlarmType.TEMPERATURE -> sensorState.latestMeasurement.temperature?.valueWithUnit
                AlarmType.HUMIDITY -> unitsConverter.getHumidityString(
                    sensorState.latestMeasurement.humidity?.original,
                    sensorState.latestMeasurement.temperature?.original,
                    UnitType.HumidityUnit.Relative
                )
                AlarmType.PRESSURE -> sensorState.latestMeasurement.pressure?.valueWithUnit
                AlarmType.CO2 -> sensorState.latestMeasurement.co2?.valueWithUnit
                AlarmType.PM10 -> sensorState.latestMeasurement.pm10?.valueWithUnit
                AlarmType.PM25 -> sensorState.latestMeasurement.pm25?.valueWithUnit
                AlarmType.PM40 -> sensorState.latestMeasurement.pm40?.valueWithUnit
                AlarmType.PM100 -> sensorState.latestMeasurement.pm100?.valueWithUnit
                AlarmType.SOUND -> sensorState.latestMeasurement.dBaAvg?.valueWithUnit
                AlarmType.LUMINOSITY -> sensorState.latestMeasurement.luminosity?.valueWithUnit
                AlarmType.VOC -> sensorState.latestMeasurement.voc?.valueWithUnit
                AlarmType.NOX -> sensorState.latestMeasurement.nox?.valueWithUnit
                AlarmType.AQI -> sensorState.latestMeasurement.aqi?.value?.roundHalfUp(0)?.toInt().toString()
                else -> null
            }
            if (latestValue != null) {
                Text(
                    modifier = Modifier.padding(all = RuuviStationTheme.dimensions.screenPadding),
                    style = RuuviStationTheme.typography.dashboardSecondary,
                    fontSize = RuuviStationTheme.fontSizes.compact,
                    text = stringResource(
                        id = R.string.latest_measured_value,
                        latestValue
                    ),
                )
            }
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

    if (openAlarmEditDialog) {
        AlarmEditDialog(
            alarmState = alarmState,
            getPossibleRange = getExtraRange,
            validateRange = validateRange,
            manualRangeSave = manualRangeSave,
            getUnit = getUnit
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
    sensorState: RuuviTag,
    changeEnabled: (AlarmType, Boolean) -> Unit,
    setDescription: (AlarmType, String) -> Unit,
    setRange: (AlarmType, ClosedFloatingPointRange<Float>) -> Unit,
    saveRange: (AlarmType) -> Unit,
    getPossibleRange: (AlarmType) -> ClosedFloatingPointRange<Float>,
    validateRange: (AlarmType, Double?, Double?) -> Boolean,
    manualRangeSave: (AlarmType, Double?, Double?) -> Unit,
    getUnit: (AlarmType) -> String
) {
    var openDescriptionDialog by remember { mutableStateOf(false) }
    var openAlarmEditDialog by remember { mutableStateOf(false) }
    val possibleRange by remember {
        mutableStateOf(getPossibleRange.invoke(alarmState.type))
    }

    ExpandableContainer(header = {
        AlarmHeader(title, alarmState)
    }) {
        SmallerText(stringResource(id = R.string.rssi_alert_description))
        SwitchIndicatorRuuvi(
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            text = getMutedText(LocalContext.current, alarmState.mutedTill),
            checked = alarmState.isEnabled.value,
            onCheckedChange = {
                changeEnabled.invoke(alarmState.type, it)
            },
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
            emptyText = "",
            applyBoldStyleToDecimals = true
        ) {
            openAlarmEditDialog = true
        }

        RuuviRangeSlider(
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            values = alarmState.rangeLow .. alarmState.rangeHigh,
            valueRange = possibleRange,
            onValueChange = {
                setRange.invoke(alarmState.type, it)
            },
            onValueChangeFinished = {
                saveRange.invoke(alarmState.type)
            }
        )
        if (sensorState.latestMeasurement != null) {
            val latestValue = sensorState.latestMeasurement.rssi.valueWithUnit
            Paragraph(
                modifier = Modifier.padding(all = RuuviStationTheme.dimensions.screenPadding),
                text = stringResource(
                    id = R.string.latest_measured_value,
                    latestValue
                )
            )
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

    if (openAlarmEditDialog) {
        AlarmEditDialog(
            alarmState = alarmState,
            getPossibleRange = getPossibleRange,
            validateRange = validateRange,
            manualRangeSave = manualRangeSave,
            getUnit = getUnit
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
        if (alarmState.isEnabled.value) {
            if (alarmState.triggered) {
                BlinkingEffect() {
                    Icon(
                        modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.mediumPlus),
                        painter = painterResource(id = R.drawable.ic_notifications_active_24px),
                        contentDescription = null,
                        tint = RuuviStationTheme.colors.activeAlertThemed
                    )
                }
            } else {
                Icon(
                    modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.mediumPlus),
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
        SmallerText(stringResource(id = R.string.alert_movement_description))
        SwitchIndicatorRuuvi(
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            text = getMutedText(LocalContext.current, alarmState.mutedTill),
            checked = alarmState.isEnabled.value,
            onCheckedChange = {
                changeEnabled.invoke(alarmState.type, it)
            },
        )
        DividerRuuvi()
        TextEditButton(
            value = alarmState.customDescription,
            emptyText = stringResource(id = R.string.alarm_custom_title_hint)
        ) {
            openDescriptionDialog = true
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChangeDescriptionDialog(
    alarmState: AlarmItemState,
    setDescription: (AlarmType, String) -> Unit,
    onDismissRequest : () -> Unit
) {
    var description by remember {
        mutableStateOf(TextFieldValue(alarmState.customDescription, TextRange(alarmState.customDescription.length)))
    }

    CustomContentDialog(
        title = stringResource(id = R.string.alarm_custom_description_title),
        onDismissRequest = onDismissRequest,
        onOkClickAction = {
            setDescription.invoke(alarmState.type, description.text)
            onDismissRequest.invoke()
        }
    ) {
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        TextFieldRuuvi(
            value = description,
            onValueChange = {
                if (it.text.length <= 32) description = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (it.isFocused) keyboardController?.show()
                },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        LaunchedEffect(key1 = Unit) {
            delay(100)
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun AlarmEditDialog(
    alarmState: AlarmItemState,
    getPossibleRange: (AlarmType) -> ClosedFloatingPointRange<Float>,
    validateRange: (AlarmType, Double?, Double?) -> Boolean,
    manualRangeSave: (AlarmType, Double?, Double?) -> Unit,
    getUnit: (AlarmType) -> String,
    onDismissRequest : () -> Unit,
) {
    val title = when (alarmState.type) {
        AlarmType.TEMPERATURE -> stringResource(id = R.string.alert_dialog_title_temperature)
        AlarmType.PRESSURE -> stringResource(id = R.string.alert_dialog_title_pressure)
        AlarmType.HUMIDITY -> stringResource(id = R.string.alert_dialog_title_humidity)
        AlarmType.RSSI -> stringResource(id = R.string.alert_dialog_title_rssi)
        else -> ""
    }

    val keyboardType = if (alarmState.type == AlarmType.TEMPERATURE || alarmState.type == AlarmType.RSSI) {
        KeyboardType.Unspecified
    } else {
        KeyboardType.Decimal
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

    CustomContentDialog(
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

        val possibleMinString =
            possibleRange.start.toInt().toString() + " " + getUnit(alarmState.type)
        Subtitle(text = stringResource(id = R.string.alert_dialog_min, possibleMinString))
        NumberTextFieldRuuvi(
            value = alarmState.displayLow,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = keyboardType),
            keyboardActions = KeyboardActions(onDone = { focusRequester.requestFocus() })
        ) { parsed, value ->
            min = value
        }

        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

        val possibleMaxString =
            possibleRange.endInclusive.toInt().toString() + " " + getUnit(alarmState.type)
        Subtitle(text = stringResource(id = R.string.alert_dialog_max, possibleMaxString))
        NumberTextFieldRuuvi(
            value = alarmState.displayHigh,
            modifier = Modifier.focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
            })
        ) { parsed, value ->
            max = value
        }
    }
}

@Composable
fun OfflineAlarmEditDialog(
    alarmState: AlarmItemState,
    manualRangeSave: (AlarmType, Double?, Double?) -> Unit,
    onDismissRequest : () -> Unit,
) {
    val title = stringResource(id = R.string.alert_cloud_connection_dialog_title)

    var delay by remember {
        mutableDoubleStateOf(alarmState.max / 60)
    }

    CustomContentDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        onOkClickAction = {
            Timber.d("Offline alert save: $delay")
            val max = delay * 60
            manualRangeSave.invoke(alarmState.type, 0.0, max)
            onDismissRequest.invoke()
        },
        validation = {
            (delay.toInt() >= 2) && (delay.toInt() <= 1440)
        }
    ) {
        Paragraph(text = stringResource(id = R.string.alert_cloud_connection_dialog_description))

        val focusManager = LocalFocusManager.current

        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))

        NumberTextFieldRuuvi(
            value = delay.toLong().toString(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
            })
        ) { parsed, value ->
            if (parsed && value != null) delay = value
        }

    }
}

@Composable
fun SmallerText(text: String) {
    SmallerParagraph(
        modifier = Modifier.padding(
            horizontal = RuuviStationTheme.dimensions.screenPadding,
            vertical = RuuviStationTheme.dimensions.mediumPlus),
        text = text
    )
}

fun getMutedText(context: Context, mutedTill: Date?): String {
    return if (mutedTill != null && mutedTill > Date()) {
        context.getString(R.string.muted_till, DateFormat.getTimeInstance(DateFormat.SHORT).format(mutedTill))
    } else {
        ""
    }
}