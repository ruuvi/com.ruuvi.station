package com.ruuvi.station.settings.ui

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings.LOAD_DEFAULT
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.components.dialog.CustomContentDialog
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.feature.data.Feature
import com.ruuvi.station.feature.data.FeatureFlag
import timber.log.Timber

@Composable
fun DeveloperSettings(
    scaffoldState: ScaffoldState,
    onNavigate: (String) -> Unit,
    viewModel: DeveloperSettingsViewModel
) {
    val devServerEnabled by viewModel.devServerEnabled.collectAsState()
    val useWebShare by viewModel.useWebShare.collectAsState()
    val dashboardCardTimeoutSeconds by viewModel.dashboardCardTimeoutSeconds.collectAsState()
    var showDashboardCardTimeoutDialog by rememberSaveable { mutableStateOf(false) }

    PageSurfaceWithPadding {
        Column() {
            FeatureSwitch(
                feature = FeatureFlag.GLOBAL_UNITS_SETTINGS,
                checked = viewModel::getFeatureState,
                onCheckedChange = viewModel::setFeatureValue
            )

            FeatureSwitch(
                feature = FeatureFlag.MARKETING_CONSENT,
                checked = viewModel::getFeatureState,
                onCheckedChange = viewModel::setFeatureValue
            )

            SwitchIndicatorRuuvi(
                text = stringResource(id = R.string.use_web_version_of_share),
                checked = useWebShare,
                onCheckedChange = viewModel::setUseWebShare
            )

            SwitchIndicatorRuuvi(
                text = stringResource(id = R.string.use_dev_server),
                checked = devServerEnabled,
                onCheckedChange = viewModel::setDevServerEnabled
            )
            Paragraph(text = stringResource(id = R.string.use_dev_server_description))

            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
            TextEditWithCaptionButton(
                title = "Sensor card timeout (seconds)",
                value = dashboardCardTimeoutSeconds.toString(),
                icon = painterResource(R.drawable.edit_20),
                tint = RuuviStationTheme.colors.accent,
                editAction = { showDashboardCardTimeoutDialog = true }
            )
            Paragraph(text = "Show a warning icon when dashboard card measurements are not updated within this period.")

            Spacer(modifier = Modifier.height(48.dp))
            Subtitle("Debug info")
            val scale = LocalDensity.current.fontScale
            Paragraph(text = stringResource(id = R.string.font_scaling, scale * 100))
            Spacer(modifier = Modifier.height(48.dp))
            RuuviButton(text = stringResource(id = R.string.disable_developer_mode)) {
                viewModel.setDevModeEnabled(false)
                onNavigate.invoke(SettingsRoutes.LIST)
            }
        }
    }

    if (showDashboardCardTimeoutDialog) {
        DashboardCardTimeoutDialog(
            initialSeconds = dashboardCardTimeoutSeconds,
            onSave = viewModel::setDashboardCardTimeoutSeconds,
            onDismissRequest = { showDashboardCardTimeoutDialog = false }
        )
    }
}

@Composable
private fun DashboardCardTimeoutDialog(
    initialSeconds: Long,
    onSave: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf(initialSeconds.toString()) }
    val seconds = parseDashboardCardTimeoutSeconds(input)

    CustomContentDialog(
        title = "Sensor card timeout (seconds)",
        validation = { seconds != null },
        onDismissRequest = onDismissRequest,
        onOkClickAction = {
            onSave(checkNotNull(seconds))
            onDismissRequest()
        }
    ) {
        Paragraph(text = "Enter a whole number of seconds. Minimum: 1 second.")
        TextFieldRuuvi(
            modifier = Modifier.fillMaxWidth(),
            value = input,
            hint = stringResource(R.string.seconds),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onValueChange = { input = it }
        )
        if (seconds == null) {
            WarningText(text = "Enter a valid whole number of at least 1 second.")
        }
    }
}

@Composable
fun FeatureSwitch(
    feature: Feature,
    checked: (Feature) -> Boolean,
    onCheckedChange: ((Feature, Boolean) -> Unit),
    modifier: Modifier = Modifier
) {
    var enabled = remember { mutableStateOf(checked.invoke(feature)) }

    SwitchIndicatorRuuvi(
        text = feature.title,
        checked = enabled.value,
        onCheckedChange = { check ->
            onCheckedChange.invoke(feature, check)
            enabled.value = checked.invoke(feature)
                          },
        modifier = modifier
    )
}
