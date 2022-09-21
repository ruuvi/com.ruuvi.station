package com.ruuvi.station.tagsettings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.ruuvi.station.R
import com.ruuvi.station.alarm.ui.AlarmItems
import com.ruuvi.station.alarm.ui.AlarmItemsViewModel
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.calibration.ui.CalibrationSettings
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
    val sensorOwnedByUserObserve by viewModel.sensorOwnedByUserObserve.collectAsState(initial = false)
    val sensorIsShared by viewModel.sensorShared.collectAsState()
    val sensorOwnedOrOfflineObserve by viewModel.sensorOwnedOrOfflineObserve.collectAsState(initial = false)

    Column() {
        GeneralSettings(
            sensorState = sensorState,
            userLoggedIn = userLoggedIn,
            sensorOwnedByUserObserve = sensorOwnedByUserObserve,
            sensorIsShared = sensorIsShared,
            setName = viewModel::setName
        )
        AlarmItems(alarmsViewModel)
        if (sensorOwnedOrOfflineObserve) {
            CalibrationSettings(
                sensorState = sensorState,
                getTemperatureOffsetString = viewModel::getTemperatureOffsetString,
                getHumidityOffsetString = viewModel::getHumidityOffsetString,
                getPressureOffsetString = viewModel::getPressureOffsetString
            )
        }
    }

    LaunchedEffect(key1 = 1) {
        viewModel.checkIfSensorShared()
    }
}

@Composable
fun GeneralSettings(
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