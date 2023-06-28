package com.ruuvi.station.nfc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.Dialog
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.components.RuuviTextButton
import com.ruuvi.station.app.ui.components.SubtitleWithPadding
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.nfc.domain.NfcScanResponse

@Composable
fun NfcDialog(
    sensorInfo: NfcScanResponse.NewSensor,
    onDismissRequest : () -> Unit,
    addSensorAction: () -> Unit
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
                    horizontalAlignment = Alignment.End
                ) {
                    RuuviTextButton(
                        text = stringResource(id = R.string.nfc_dialog_add_sensor),
                        onClick = {
                            addSensorAction.invoke()
                        }
                    )
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