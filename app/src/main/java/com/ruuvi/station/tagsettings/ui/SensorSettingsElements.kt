package com.ruuvi.station.tagsettings.ui

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ruuvi.station.R
import com.ruuvi.station.alarm.ui.AlarmsGroup
import com.ruuvi.station.alarm.ui.AlarmItemsViewModel
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.calibration.ui.CalibrationSettingsGroup
import com.ruuvi.station.dfu.ui.FirmwareGroup
import com.ruuvi.station.network.ui.ClaimSensorActivity
import com.ruuvi.station.network.ui.ShareSensorActivity
import com.ruuvi.station.tag.domain.RuuviTag

@Composable
fun SensorSettings(
    viewModel: TagSettingsViewModel,
    alarmsViewModel: AlarmItemsViewModel
) {
    val sensorState by viewModel.sensorState.collectAsState()
    val userLoggedIn by viewModel.userLoggedIn.collectAsState()
    val sensorOwnedByUser by viewModel.sensorOwnedByUser.collectAsState(initial = false)
    val sensorIsShared by viewModel.sensorShared.collectAsState()
    val sensorOwnedOrOffline by viewModel.sensorOwnedOrOffline.collectAsState(initial = false)
    val isLowBattery by viewModel.isLowBattery.collectAsState(initial = false)
    val firmware by viewModel.firmware.collectAsState(initial = null)

    Column {
        GeneralSettingsGroup(
            sensorState = sensorState,
            userLoggedIn = userLoggedIn,
            sensorOwnedByUserObserve = sensorOwnedByUser,
            sensorIsShared = sensorIsShared,
            setName = viewModel::setName
        )
        AlarmsGroup(alarmsViewModel)
        if (sensorOwnedOrOffline) {
            CalibrationSettingsGroup(
                sensorState = sensorState,
                getTemperatureOffsetString = viewModel::getTemperatureOffsetString,
                getHumidityOffsetString = viewModel::getHumidityOffsetString,
                getPressureOffsetString = viewModel::getPressureOffsetString
            )
        }
        DividerRuuvi()
        MoreInfoGroup(
            sensorState = sensorState,
            isLowBattery = isLowBattery,
            getAccelerationString = viewModel::getAccelerationString,
            getSignalString = viewModel::getSignalString
        )
        if (sensorOwnedOrOffline) {
            DividerRuuvi()
            FirmwareGroup(
                sensorState = sensorState,
                firmware = firmware
            )
        }
        DividerRuuvi()
        RemoveGroup(
            sensorState = sensorState,
            sensorOwnedByUser = sensorOwnedByUser,
            deleteSensor = viewModel::deleteSensor
        )
    }

    LaunchedEffect(key1 = 1) {
        viewModel.checkIfSensorShared()
        viewModel.updateSensorFirmwareVersion()
    }
}

@Composable
fun GeneralSettingsGroup(
    sensorState: RuuviTag,
    userLoggedIn: Boolean,
    sensorOwnedByUserObserve: Boolean,
    sensorIsShared: Boolean,
    setName: (String) -> Unit
) {
    val context = LocalContext.current
    var setNameDialog by remember { mutableStateOf(false) }

    SensorSettingsTitle(title = stringResource(id = R.string.general))
    TextEditWithCaptionButton(
        title = stringResource(id = R.string.tag_name),
        value = sensorState.displayName
    ) {
        setNameDialog = true
    }
    if (userLoggedIn) {
        DividerRuuvi()
        if (sensorState.owner.isNullOrEmpty()) {
            TextEditWithCaptionButton(
                title = stringResource(id = R.string.tagsettings_owner),
                value = stringResource(id = R.string.owner_none),
                icon = painterResource(id = R.drawable.arrow_forward_16)
            ) {
                ClaimSensorActivity.start(context, sensorState.id)
            }
        } else {
            ValueWithCaption(
                title = stringResource(id = R.string.tagsettings_owner),
                value = sensorState.owner
            )
        }
        if (sensorOwnedByUserObserve) {
            val sharedText = if (sensorIsShared) {
                stringResource(R.string.sensor_shared)
            } else {
                stringResource(R.string.sensor_not_shared)
            }
            DividerRuuvi()

            TextEditWithCaptionButton(
                title = stringResource(id = R.string.share),
                value = sharedText,
                icon = painterResource(id = R.drawable.arrow_forward_16)
            ) {
                ShareSensorActivity.start(context, sensorState.id)
            }
        }
    }

    if (setNameDialog) {
        SetSensorName(
            value = sensorState.name,
            setName = setName,
        ) {
            setNameDialog = false
        }
    }
}

@Composable
fun SetSensorName(
    value: String?,
    setName: (String) -> Unit,
    onDismissRequest : () -> Unit
) {
    var name by remember {
        mutableStateOf(value ?: "")
    }
    RuuviDialog(
        title = stringResource(id = R.string.tag_name),
        onDismissRequest = onDismissRequest,
        onOkClickAction = {
            setName.invoke(name)
            onDismissRequest.invoke()
        }
    ) {
        ParagraphWithPadding(text = stringResource(id = R.string.rename_sensor_message))
        TextFieldRuuvi(
            value = name,
            onValueChange = {
                name = it
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
    }
}



@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RemoveGroup(
    sensorState: RuuviTag,
    sensorOwnedByUser: Boolean,
    deleteSensor: ()->Unit
) {
    val activity = (LocalContext.current as? Activity)

    val removeMessage = if (sensorState.networkSensor != true) {
        stringResource(id = R.string.remove_local_sensor)
    } else {
        if (sensorOwnedByUser) {
            stringResource(id = R.string.remove_claimed_sensor)
        } else {
            stringResource(id = R.string.remove_shared_sensor)
        }
    }

    var removeDialog by remember {
        mutableStateOf(false)
    }

    ExpandableContainer(header = {
        Text(
            text = stringResource(id = R.string.remove),
            style = RuuviStationTheme.typography.title,
        )
    },
        backgroundColor = RuuviStationTheme.colors.settingsTitle
    ) {
        TextEditWithCaptionButton(
            value = null,
            title = stringResource(id = R.string.remove_this_sensor),
            icon = painterResource(id = R.drawable.arrow_forward_16)
        ) {
            removeDialog = true
        }
    }

    if (removeDialog) {
        RuuviDialog(
            title = stringResource(id = R.string.tagsettings_sensor_remove),
            onDismissRequest = { removeDialog = false },
            onOkClickAction = {
                deleteSensor()
                activity?.finish()
            }
        ) {
            Paragraph(text = removeMessage)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MoreInfoGroup(
    sensorState: RuuviTag,
    isLowBattery: Boolean,
    getAccelerationString: (Double?) -> String,
    getSignalString: (Int) -> String
) {
    ExpandableContainer(header = {
        Text(
            text = stringResource(id = R.string.more_info),
            style = RuuviStationTheme.typography.title,
        )
    },
        backgroundColor = RuuviStationTheme.colors.settingsTitle
    ) {
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
        MoreInfoItem(
            title = stringResource(id = R.string.mac_address),
            value = sensorState.id
        )
        if (sensorState.dataFormat == 3 || sensorState.dataFormat == 5) {
            MoreInfoItem(
                title = stringResource(id = R.string.data_format),
                value = sensorState.dataFormat.toString()
            )
            BatteryInfoItem(
                voltage = sensorState.voltage,
                isLowBattery = isLowBattery
            )
            MoreInfoItem(
                title = stringResource(id = R.string.acceleration_x),
                value = getAccelerationString(sensorState.accelerationX)
            )
            MoreInfoItem(
                title = stringResource(id = R.string.acceleration_y),
                value = getAccelerationString(sensorState.accelerationY)
            )
            MoreInfoItem(
                title = stringResource(id = R.string.acceleration_z),
                value = getAccelerationString(sensorState.accelerationZ)
            )
            MoreInfoItem(
                title = stringResource(id = R.string.tx_power),
                value = stringResource(id = R.string.tx_power_reading, sensorState.txPower)
            )
            MoreInfoItem(
                title = stringResource(id = R.string.rssi),
                value = getSignalString(sensorState.rssi)
            )
            MoreInfoItem(
                title = stringResource(id = R.string.measurement_sequence_number),
                value = sensorState.measurementSequenceNumber.toString()
            )
        }
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoreInfoItem (
    title: String,
    value: String
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    Row(modifier = Modifier.padding(vertical = RuuviStationTheme.dimensions.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.medium)
                .weight(1f),
            style = RuuviStationTheme.typography.paragraph,
            textAlign = TextAlign.Start,
            text = title)
        Text(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.medium)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        clipboardManager.setText(AnnotatedString(value))
                    }
                ),
            style = RuuviStationTheme.typography.paragraph,
            textAlign = TextAlign.End,
            text = value)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BatteryInfoItem (
    voltage: Double,
    isLowBattery: Boolean
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val value = stringResource(id = R.string.voltage_reading, voltage, stringResource(id = R.string.voltage_unit)) 

    Row(modifier = Modifier.padding(vertical = RuuviStationTheme.dimensions.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.medium)
                .weight(1f),
            style = RuuviStationTheme.typography.paragraph,
            textAlign = TextAlign.Start,
            text = stringResource(id = R.string.battery_voltage))
        if (isLowBattery) {
            Text(
                modifier = Modifier
                    .padding(horizontal = RuuviStationTheme.dimensions.small),
                style = RuuviStationTheme.typography.warning,
                textAlign = TextAlign.Start,
                text = stringResource(id = R.string.brackets_text, stringResource(id = R.string.replace_battery)))
        } else {
            Text(
                modifier = Modifier
                    .padding(horizontal = RuuviStationTheme.dimensions.small),
                style = RuuviStationTheme.typography.success,
                textAlign = TextAlign.Start,
                text = stringResource(id = R.string.brackets_text, stringResource(id = R.string.battery_ok)))
        }

        Text(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.medium)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        clipboardManager.setText(AnnotatedString(value))
                    }
                ),
            style = RuuviStationTheme.typography.paragraph,
            textAlign = TextAlign.End,
            text = value)
    }
}

@Composable
@Preview
fun preview() {
    Column() {
        MoreInfoItem(
            title = stringResource(id = R.string.mac_address),
            value = "sensorState.id"
        )
        MoreInfoItem(
            title = stringResource(id = R.string.mac_address),
            value = "fasfasasdafs"
        )
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

@Composable
fun ValueWithCaption(
    title: String,
    value: String?
) {
    Row(modifier = Modifier
        .height(RuuviStationTheme.dimensions.sensorSettingTitleHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.medium),
            style = RuuviStationTheme.typography.subtitle,
            textAlign = TextAlign.Start,
            text = title)
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RuuviStationTheme.dimensions.medium),
            style = RuuviStationTheme.typography.paragraph,
            textAlign = TextAlign.End,
            text = value ?: "")
    }
}