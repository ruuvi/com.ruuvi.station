package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.ruuvi.station.R
import com.ruuvi.station.alarm.ui.AlarmsGroup
import com.ruuvi.station.alarm.ui.AlarmItemsViewModel
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.calibration.ui.CalibrationSettingsGroup
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.dfu.ui.FirmwareGroup
import com.ruuvi.station.network.ui.claim.ClaimSensorActivity
import com.ruuvi.station.network.ui.ShareSensorActivity
import com.ruuvi.station.tag.domain.RuuviTag
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun SensorSettings(
    scaffoldState: ScaffoldState,
    viewModel: TagSettingsViewModel,
    alarmsViewModel: AlarmItemsViewModel
) {
    val context = LocalContext.current
    val sensorState by viewModel.sensorState.collectAsState()
    val userLoggedIn by viewModel.userLoggedIn.collectAsState()
    val sensorOwnedByUser by viewModel.sensorOwnedByUser.collectAsState(initial = false)
    val sensorIsShared by viewModel.sensorShared.collectAsState()
    val sensorOwnedOrOffline by viewModel.sensorOwnedOrOffline.collectAsState(initial = false)
    val isLowBattery by viewModel.isLowBattery.collectAsState(initial = false)
    val firmware by viewModel.firmware.collectAsState(initial = null)

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        SensorSettingsImage(sensorState = sensorState) {
            BackgroundActivity.start(context, sensorState.id)
        }
        GeneralSettingsGroup(
            sensorState = sensorState,
            userLoggedIn = userLoggedIn,
            sensorOwnedByUser = sensorOwnedByUser,
            sensorIsShared = sensorIsShared,
            setName = viewModel::setName
        )
        AlarmsGroup(alarmsViewModel)
        if (sensorOwnedOrOffline && sensorState.latestMeasurement != null) {
            CalibrationSettingsGroup(
                sensorState = sensorState,
                getTemperatureOffsetString = viewModel::getTemperatureOffsetString,
                getHumidityOffsetString = viewModel::getHumidityOffsetString,
                getPressureOffsetString = viewModel::getPressureOffsetString
            )
            DividerSurfaceColor()
        }
        MoreInfoGroup(
            sensorState = sensorState,
            isLowBattery = isLowBattery,
            getAccelerationString = viewModel::getAccelerationString,
            getSignalString = viewModel::getSignalString
        )
        DividerSurfaceColor()
        FirmwareGroup(
            sensorState = sensorState,
            firmware = firmware
        )
        DividerSurfaceColor()
        RemoveGroup(
            sensorState = sensorState,
            sensorOwnedByUser = sensorOwnedByUser,
            deleteSensor = viewModel::deleteSensor
        )
    }

    LaunchedEffect(key1 = 1) {
        viewModel.updateSensorFirmwareVersion()
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SensorSettingsImage(
    sensorState: RuuviTag,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(color = RuuviStationTheme.colors.defaultSensorBackground)
            .clickable { onClick.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Timber.d("Image path ${sensorState.userBackground} ")

        if (sensorState.userBackground != null) {
            val uri = Uri.parse(sensorState.userBackground)

            if (uri.path != null) {
                GlideImage(
                    modifier = Modifier.fillMaxSize(),
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = R.drawable.tag_bg_layer),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
        }
    }

    TextEditWithCaptionButton(
        title = stringResource(id = R.string.background_image),
        icon = painterResource(id = R.drawable.camera_24),
        tint = RuuviStationTheme.colors.accent
    ) {
        BackgroundActivity.start(context, sensorState.id)
    }
}

@Composable
fun GeneralSettingsGroup(
    sensorState: RuuviTag,
    userLoggedIn: Boolean,
    sensorOwnedByUser: Boolean,
    sensorIsShared: UiText,
    setName: (String) -> Unit
) {
    val context = LocalContext.current
    var setNameDialog by remember { mutableStateOf(false) }

    DividerRuuvi()
    TextEditWithCaptionButton(
        title = stringResource(id = R.string.tag_name),
        value = sensorState.displayName,
        icon = painterResource(id = R.drawable.edit_20),
        tint = RuuviStationTheme.colors.accent
    ) {
        setNameDialog = true
    }
    if (userLoggedIn) {
        DividerRuuvi()
        val owner = if (sensorState.owner.isNullOrEmpty()) {
            stringResource(id = R.string.owner_none)
        } else {
            sensorState.owner
        }
        TextEditWithCaptionButton(
            title = stringResource(id = R.string.tagsettings_owner),
            value = owner,
            icon = painterResource(id = R.drawable.arrow_forward_16),
            tint = RuuviStationTheme.colors.trackInactive
        ) {
            ClaimSensorActivity.start(context, sensorState.id)
        }
        if (sensorOwnedByUser) {
            val sharedText = sensorIsShared.asString(context)
            DividerRuuvi()

            TextEditWithCaptionButton(
                title = stringResource(id = R.string.share),
                value = sharedText,
                icon = painterResource(id = R.drawable.arrow_forward_16),
                tint = RuuviStationTheme.colors.trackInactive
            ) {
                ShareSensorActivity.start(context, sensorState.id)
            }
        }
    } else {
        DividerRuuvi()
        TextEditWithCaptionButton(
            title = stringResource(id = R.string.tagsettings_owner),
            value = stringResource(id = R.string.owner_none),
            icon = painterResource(id = R.drawable.arrow_forward_16),
            tint = RuuviStationTheme.colors.trackInactive
        ) {
            ClaimSensorActivity.start(context, sensorState.id)
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SetSensorName(
    value: String?,
    setName: (String) -> Unit,
    onDismissRequest : () -> Unit
) {
    var name by remember {
        mutableStateOf(TextFieldValue(value ?: "", TextRange((value ?: "").length)))
    }
    RuuviDialog(
        title = stringResource(id = R.string.tag_name),
        onDismissRequest = onDismissRequest,
        onOkClickAction = {
            setName.invoke(name.text)
            onDismissRequest.invoke()
        }
    ) {
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        ParagraphWithPadding(text = stringResource(id = R.string.rename_sensor_message))
        TextFieldRuuvi(
            value = name,
            onValueChange = {
                if (it.text.length <= 32) name = it
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
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }
            )
        )

        LaunchedEffect(key1 = Unit) {
            delay(100)
            focusRequester.requestFocus()
        }
    }
}



@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RemoveGroup(
    sensorState: RuuviTag,
    sensorOwnedByUser: Boolean,
    deleteSensor: ()->Unit
) {
    val context = LocalContext.current
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
            title = stringResource(id = R.string.remove_this_sensor),
            icon = painterResource(id = R.drawable.arrow_forward_16),
            tint = RuuviStationTheme.colors.trackInactive
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
                DashboardActivity.start(context)
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
        if (sensorState.latestMeasurement?.dataFormat == 3 || sensorState.latestMeasurement?.dataFormat == 5) {
            MoreInfoItem(
                title = stringResource(id = R.string.data_format),
                value = sensorState.latestMeasurement.dataFormat.toString()
            )
            BatteryInfoItem(
                voltage = sensorState.latestMeasurement.voltageValue.value,
                isLowBattery = isLowBattery
            )
            MoreInfoItem(
                title = stringResource(id = R.string.acceleration_x),
                value = getAccelerationString(sensorState.latestMeasurement.accelerationX)
            )
            MoreInfoItem(
                title = stringResource(id = R.string.acceleration_y),
                value = getAccelerationString(sensorState.latestMeasurement.accelerationY)
            )
            MoreInfoItem(
                title = stringResource(id = R.string.acceleration_z),
                value = getAccelerationString(sensorState.latestMeasurement.accelerationZ)
            )
            MoreInfoItem(
                title = stringResource(id = R.string.tx_power),
                value = stringResource(id = R.string.tx_power_reading, sensorState.latestMeasurement.txPower)
            )
            MoreInfoItem(
                title = stringResource(id = R.string.signal_strength_rssi),
                value = sensorState.latestMeasurement.rssiValue.valueWithUnit
            )
            MoreInfoItem(
                title = stringResource(id = R.string.measurement_sequence_number),
                value = sensorState.latestMeasurement.measurementSequenceNumber.toString()
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
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
                .weight(1f),
            style = RuuviStationTheme.typography.paragraph,
            textAlign = TextAlign.Start,
            text = title)
        Text(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
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
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
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
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
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
            modifier = Modifier.padding(RuuviStationTheme.dimensions.screenPadding)
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
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            style = RuuviStationTheme.typography.subtitle,
            textAlign = TextAlign.Start,
            text = title)
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            style = RuuviStationTheme.typography.paragraph,
            textAlign = TextAlign.End,
            text = value ?: "")
    }
}