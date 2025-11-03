package com.ruuvi.station.app.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.ParagraphWithPadding
import com.ruuvi.station.app.ui.components.RuuviTextButton
import com.ruuvi.station.app.ui.components.SubtitleWithPadding
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun RuuviPermissionDialog(
    title: String = "",
    message: String = "",
    onAccept: () -> Unit,
    onDismissRequest: () -> Unit,
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
                if (title.isNotEmpty()) {
                    SubtitleWithPadding(text = title)
                }

                if (message.isNotEmpty()) {
                    ParagraphWithPadding(text = message)
                }

                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RuuviTextButton(
                        text = stringResource(id = R.string.agree),
                        onClick = {
                            onDismissRequest.invoke()
                            onAccept.invoke()
                        }
                    )
                    RuuviTextButton(
                        text = stringResource(id = R.string.decline),
                        onClick = {
                            onDismissRequest.invoke()
                        }
                    )
                }
            }
        }
    }
}