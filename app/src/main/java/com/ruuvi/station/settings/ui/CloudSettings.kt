package com.ruuvi.station.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.PageSurfaceWithPadding
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.components.SwitchIndicatorRuuvi

@Composable
fun CloudSettings(
    scaffoldState: ScaffoldState,
    viewModel: CloudSettingsViewModel
) {

    val cloudModeEnabled = viewModel.cloudModeEnabled.collectAsState()

    PageSurfaceWithPadding {
        Column() {
            SwitchIndicatorRuuvi(
                text = stringResource(id = R.string.cloud_only_mode),
                checked = cloudModeEnabled.value,
                onCheckedChange = viewModel::setIsCloudModeEnabled
            )
            Paragraph(text = stringResource(id = R.string.cloud_only_mode_description))
        }
    }
}