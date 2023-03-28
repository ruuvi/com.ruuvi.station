package com.ruuvi.station.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlertNotificationInteractor
import com.ruuvi.station.app.ui.components.PageSurface
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun AlertNotificationsSettings(
    scaffoldState: ScaffoldState
) {
    val context = LocalContext.current
    PageSurface() {
        Column() {
            SettingsElement(
                name = stringResource(id = R.string.settings_sound),
                description = null,
                onClick = {
                    AlertNotificationInteractor.openNotificationChannelSettings(context)
                }
            )
            Paragraph(
                text = stringResource(id = R.string.settings_sound_description),
                modifier = Modifier.padding(RuuviStationTheme.dimensions.screenPadding)
            )
        }
    }
}