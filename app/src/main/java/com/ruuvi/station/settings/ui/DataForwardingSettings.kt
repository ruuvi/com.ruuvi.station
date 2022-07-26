package com.ruuvi.station.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.settings.domain.GatewayTestResultType

@Composable
fun DataForwardingSettings(
    scaffoldState: ScaffoldState,
    viewModel: DataForwardingSettingsViewModel
) {
    val context = LocalContext.current
    val dataForwardingUrl = viewModel.dataForwardingUrl.collectAsState()
    val deviceId = viewModel.deviceId.collectAsState()
    val locationEnabled = viewModel.dataForwardingLocationEnabled.collectAsState()
    val sendDuringSync = viewModel.dataForwardingDuringSyncEnabled.collectAsState()
    val testResult = viewModel.testGatewayResult.collectAsState()

    PageSurfaceWithPadding {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))

            Subtitle(text = stringResource(id = R.string.data_forwarding_url))

            TextFieldRuuvi(
                value = dataForwardingUrl.value,
                hint = stringResource(id = R.string.data_forwarding_url_hint),
                onValueChange = viewModel::setDataForwardingUrl,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

            Subtitle(text = stringResource(id = R.string.settings_gateway_device_identifier))

            TextFieldRuuvi(
                value = deviceId.value,
                onValueChange = viewModel::setDeviceId,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

            SwitchRuuvi(
                text = stringResource(id = R.string.data_forwarding_location_enable),
                checked = locationEnabled.value,
                onCheckedChange = viewModel::setDataForwardingLocationEnabled
            )

            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))

            SwitchRuuvi(
                text = stringResource(id = R.string.data_forwarding_during_sync_enable),
                checked = sendDuringSync.value,
                onCheckedChange = viewModel::setDataForwardingDuringSyncEnabled
            )

            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                RuuviButton(text = stringResource(id = R.string.settings_data_forwarding_test)) {
                    viewModel.testGateway()
                }
                when (testResult.value.type) {
                    GatewayTestResultType.TESTING -> {
                        ParagraphWithPadding(text = stringResource(id = R.string.gateway_testing))
                    }
                    GatewayTestResultType.SUCCESS -> {
                        ParagraphWithPadding(text = stringResource(id = R.string.gateway_test_success, testResult.value.code ?: 0))
                    }
                    GatewayTestResultType.FAIL -> {
                        ParagraphWithPadding(text = stringResource(id = R.string.gateway_test_fail, testResult.value.code ?: 0))
                    }
                    GatewayTestResultType.EXCEPTION -> {
                        ParagraphWithPadding(text = stringResource(id = R.string.gateway_test_exception))
                    }
                    else -> {}
                }

            }
            
            ParagraphWithPadding(text = stringResource(id = R.string.settings_gateway_details))
        }
    }
}