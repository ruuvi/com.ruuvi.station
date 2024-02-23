package com.ruuvi.station.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.*

@Composable
fun DeveloperSettings(
    scaffoldState: ScaffoldState,
    viewModel: DeveloperSettingsViewModel
) {
    val devServerEnabled = viewModel.devServerEnabled.collectAsState()

    PageSurfaceWithPadding {
        Column() {
            SwitchIndicatorRuuvi(
                text = stringResource(id = R.string.use_dev_server),
                checked = devServerEnabled.value,
                onCheckedChange = viewModel::setDevServerEnabled
            )
            Paragraph(text = stringResource(id = R.string.use_dev_server_description))
        }
    }
}