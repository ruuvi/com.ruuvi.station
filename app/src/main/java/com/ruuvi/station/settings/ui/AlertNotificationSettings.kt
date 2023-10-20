package com.ruuvi.station.settings.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlertNotificationInteractor
import com.ruuvi.station.app.ui.components.DividerRuuvi
import com.ruuvi.station.app.ui.components.PageSurface
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.components.SwitchRuuvi
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun AlertNotificationsSettings(
    scaffoldState: ScaffoldState,
    viewModel: AlertNotificationsSettingsViewModel
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Timber.d("Granted")
            AlertNotificationInteractor.openNotificationChannelSettings(context)
        } else {
            Timber.d("Not granted")
            CoroutineScope(Dispatchers.Main).launch {
                scaffoldState.snackbarHostState.showSnackbar(context.getString(R.string.permission_notification_needed))
            }
        }
    }

    var settingsClicked by remember { mutableStateOf(false) }

    val limitLocalAlerts = viewModel.limitLocalAlerts.collectAsState()

    PageSurface() {
        Column() {
            Column (modifier = Modifier
                .padding(RuuviStationTheme.dimensions.screenPadding)) {
                SwitchRuuvi(
                    text = stringResource(id = R.string.settings_alert_limit_notification),
                    checked = limitLocalAlerts.value,
                    onCheckedChange = viewModel::setLimitLocalAlerts
                )
                Paragraph(text = stringResource(id = R.string.settings_alert_limit_notification_description))
            }

            DividerRuuvi()
            SettingsElement(
                name = stringResource(id = R.string.settings_sound),
                fixedHeight = false,
                description = stringResource(id = R.string.settings_sound_description),
                onClick = {
                    settingsClicked = true
                }
            )
        }
    }

    if (settingsClicked) {
        settingsClicked = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED )
            {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                AlertNotificationInteractor.openNotificationChannelSettings(context)
            }
        } else {
            AlertNotificationInteractor.openNotificationChannelSettings(context)
        }
    }
}