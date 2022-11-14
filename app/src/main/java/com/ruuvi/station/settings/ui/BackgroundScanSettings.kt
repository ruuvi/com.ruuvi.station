package com.ruuvi.station.settings.ui

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BackgroundScanSettings(
    scaffoldState: ScaffoldState,
    viewModel: BackgroundScanSettingsViewModel
) {
    val notificationPermissionState = rememberPermissionState(
        Manifest.permission.POST_NOTIFICATIONS
    )

    val backgroundInterval = viewModel.intervalFlow.collectAsState()
    val intervalOptions = remember {
        viewModel.getIntervalOptions()
    }

    val initialValue = intervalOptions.firstOrNull { it.value == backgroundInterval.value }

    val backgroundScanEnabled by viewModel.backgroundScanEnabled.collectAsState()

    if (backgroundScanEnabled && !notificationPermissionState.status.isGranted) {
        LaunchedEffect(key1 = true) {
            notificationPermissionState.launchPermissionRequest()
        }
    }

    val showOptimizationTips = viewModel.showOptimizationTips.collectAsState()

    PageSurfaceWithPadding {
        Column() {
            SwitchRuuvi(
                text = stringResource(id = R.string.background_scanning),
                checked = backgroundScanEnabled,
                onCheckedChange = viewModel::setBackgroundScanEnabled
            )
            SubtitleWithPadding(text = stringResource(id = R.string.settings_background_scan_interval))
            TwoButtonsSelector(
                values = intervalOptions,
                initialValue = initialValue ?: intervalOptions.first(),
                onValueChanged = viewModel::setBackgroundScanInterval
            )

            if (showOptimizationTips.value) {
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
                SubtitleWithPadding(text = stringResource(id = R.string.settings_background_battery_optimization_title))
                ParagraphWithPadding(text = stringResource(id = R.string.settings_background_battery_optimization))
                ParagraphWithPadding(text = stringResource(id = viewModel.getBatteryOptimizationMessageId()))
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RuuviButton(text = stringResource(id = R.string.open_settings)) {
                        viewModel.openOptimizationSettings()
                    }
                }
            }
        }
    }
}