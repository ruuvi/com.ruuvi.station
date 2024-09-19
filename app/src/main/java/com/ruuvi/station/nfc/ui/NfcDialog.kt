package com.ruuvi.station.nfc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ruuvi.gateway.tester.nfc.model.SensorNfсScanInfo
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.components.RuuviTextButton
import com.ruuvi.station.app.ui.components.SubtitleWithPadding
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.nfc.NfcScanReciever
import com.ruuvi.station.nfc.domain.NfcScanResponse
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
import com.ruuvi.station.tagsettings.ui.TagSettingsActivity
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun NfcInteractor(
    getNfcScanResponse: (SensorNfсScanInfo) -> NfcScanResponse,
    addSensor: (String) -> Unit
) {
    val context = LocalContext.current
    var nfcDialog by remember { mutableStateOf(false) }
    var nfcScanResponse by remember { mutableStateOf<NfcScanResponse?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current


    LaunchedEffect(key1 = lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycleScope.launch {
            Timber.d("nfc scanned launch")

            NfcScanReciever.nfcSensorScanned
                .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { scanInfo ->
                    Timber.d("nfc scanned: $scanInfo")
                    if (scanInfo != null) {
                        val response = getNfcScanResponse.invoke(scanInfo)
                        Timber.d("nfc scanned response: $response")
                        nfcScanResponse = response
                        nfcDialog = true
                    }
                }
        }
    }

    if (nfcDialog && nfcScanResponse != null) {
        val response = nfcScanResponse
        if (response != null) {
            NfcDialog(
                sensorInfo = response,
                addSensorAction = {
                    addSensor(response.sensorId)
                    TagSettingsActivity.startAfterAddingNewSensor(context, response.sensorId)
                },
                goToSensorAction = {
                    SensorCardActivity.startWithDashboard(context, response.sensorId)
                },
                onDismissRequest = {
                    nfcDialog = false
                    nfcScanResponse = null
                }
            )
        }
    }
}

@Composable
fun NfcDialog(
    sensorInfo: NfcScanResponse,
    onDismissRequest : () -> Unit,
    addSensorAction: () -> Unit,
    goToSensorAction: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.extended)
                .fillMaxWidth(),
            shape = RoundedCornerShape(RuuviStationTheme.dimensions.medium),
            backgroundColor = RuuviStationTheme.colors.background
        )
        {
            Column(
                modifier = Modifier
                    .padding(all = RuuviStationTheme.dimensions.extended)
            ) {

                SubtitleWithPadding(text = stringResource(id = R.string.sensor_details))

                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

                ValueElement(
                    name = stringResource(id = R.string.nfc_dialog_name),
                    value = sensorInfo.name
                )

                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))

                ValueElement(
                    name = stringResource(id = R.string.nfc_dialog_mac_address),
                    value = sensorInfo.sensorId
                )

                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))

                ValueElement(
                    name = stringResource(id = R.string.nfc_dialog_unique_id),
                    value = sensorInfo.id
                )

                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))

                ValueElement(
                    name = stringResource(id = R.string.nfc_dialog_firmware_version),
                    value = sensorInfo.firmware
                )

                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (sensorInfo.existingSensor) {
                        RuuviTextButton(
                            text = stringResource(R.string.nfc_dialog_go_to_sensor),
                            onClick = {
                                goToSensorAction.invoke()
                            }
                        )
                    } else if (sensorInfo.canBeAdded){
                        RuuviTextButton(
                            text = stringResource(id = R.string.nfc_dialog_add_sensor),
                            onClick = {
                                addSensorAction.invoke()
                            }
                        )
                    } else {
                        SubtitleWithPadding(text = stringResource(id = R.string.nfc_enable_bt_to_add_sensor))
                    }

                    Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.extended))
                    RuuviTextButton(
                        text = stringResource(id = R.string.close),
                        onClick = {
                            onDismissRequest.invoke()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ValueElement(
    name: String,
    value: String
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier.clickable { clipboardManager.setText(AnnotatedString(value)) },
        horizontalAlignment = Alignment.Start
    ) {
        Paragraph(text = name)
        Paragraph(text = value)
    }
}