package com.ruuvi.station.dfu.ui

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.app.ui.components.DividerRuuvi
import com.ruuvi.station.app.ui.components.ExpandableContainer
import com.ruuvi.station.app.ui.components.TextEditWithCaptionButton
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagsettings.ui.ValueWithCaption

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FirmwareGroup(
    sensorState: RuuviTag,
    firmware: UiText?
) {
    val context = LocalContext.current
    val firmwareVersion = firmware?.asString(context)

    ExpandableContainer(header = {
        Text(
            text = stringResource(id = R.string.firmware),
            style = RuuviStationTheme.typography.title,
        )
    },
        backgroundColor = RuuviStationTheme.colors.settingsTitle
    ) {
        if (firmware != null) {
            ValueWithCaption(value = firmwareVersion, title = stringResource(id = R.string.current_version))
            DividerRuuvi()
        }

        TextEditWithCaptionButton(
            title = stringResource(id = R.string.firmware_update),
            icon = painterResource(id = R.drawable.arrow_forward_16),
            tint = RuuviStationTheme.colors.trackInactive
        ) {
            DfuUpdateActivity.start(context, sensorState.id)
        }
    }
}