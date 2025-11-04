package com.ruuvi.station.app.ui.components.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.RuuviTextButton
import com.ruuvi.station.app.ui.components.SubtitleWithPadding
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun CustomContentDialog(
    title: String,
    validation: () -> Boolean = { true },
    onDismissRequest : () -> Unit,
    onOkClickAction: () -> Unit,
    positiveButtonText: String = stringResource(id = R.string.ok),
    negativeButtonText: String = stringResource(id = R.string.cancel),
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
                .fillMaxWidth(),
            shape = RoundedCornerShape(RuuviStationTheme.dimensions.medium),
            backgroundColor = RuuviStationTheme.colors.background
        )
        {
            Column(
                modifier = Modifier
                    .padding(all = RuuviStationTheme.dimensions.extended)
            ) {
                if (title.isNotEmpty()) {
                    SubtitleWithPadding(text = title)
                }

                content.invoke()

                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    RuuviTextButton(
                        text = negativeButtonText,
                        onClick = {
                            onDismissRequest.invoke()
                        }
                    )

                    Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.extended))

                    RuuviTextButton(
                        text = positiveButtonText,
                        enabled = validation.invoke(),
                        onClick = {
                            onOkClickAction.invoke()
                        }
                    )
                }
            }
        }
    }
}