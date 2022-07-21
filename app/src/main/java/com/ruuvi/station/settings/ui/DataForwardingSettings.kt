package com.ruuvi.station.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.ScaffoldState
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.PageSurfaceWithPadding
import com.ruuvi.station.app.ui.components.SubtitleWithPadding

@Composable
fun DataForwardingSettings(
    scaffoldState: ScaffoldState,
    viewModel: DataForwardingSettingsViewModel
) {
    val dataForwardingUrl = viewModel.dataForwardingUrl.collectAsState()

    PageSurfaceWithPadding {
        Column() {
            SubtitleWithPadding(text = stringResource(id = R.string.data_forwarding_url))

            TextField(
                value = dataForwardingUrl.value,
                onValueChange = viewModel::setDataForwardingUrl
            )
        }
    }
}