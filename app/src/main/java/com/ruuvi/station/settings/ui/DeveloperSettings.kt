package com.ruuvi.station.settings.ui

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings.LOAD_DEFAULT
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.MIN_DASHBOARD_CARD_TIMEOUT_SECONDS
import com.ruuvi.station.app.ui.components.*
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
            DashboardCardTimeoutCounter(
                timeoutSeconds = dashboardCardTimeoutSeconds,
                onTimeoutChanged = viewModel::setDashboardCardTimeoutSeconds,
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

}

@Composable
private fun DashboardCardTimeoutCounter(
    timeoutSeconds: Long,
    onTimeoutChanged: (Long) -> Unit,
) {
    val canDecrease = timeoutSeconds > DASHBOARD_CARD_TIMEOUT_STEPS.first()
    val canIncrease = timeoutSeconds < DASHBOARD_CARD_TIMEOUT_STEPS.last()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = RuuviStationTheme.dimensions.sensorSettingTitleHeight)
            .padding(vertical = RuuviStationTheme.dimensions.mediumPlus),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            text = "Sensor card timeout",
            style = RuuviStationTheme.typography.subtitle,
            maxLines = 2,
        )
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeoutCounterButton(
                enabled = canDecrease,
                onClick = {
                    onTimeoutChanged(previousDashboardCardTimeout(timeoutSeconds))
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease sensor card timeout",
                )
            }
            Text(
                modifier = Modifier.widthIn(min = 72.dp),
                text = formatDashboardCardTimeout(timeoutSeconds),
                style = RuuviStationTheme.typography.paragraph,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            TimeoutCounterButton(
                enabled = canIncrease,
                onClick = {
                    onTimeoutChanged(nextDashboardCardTimeout(timeoutSeconds))
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase sensor card timeout",
                )
            }
        }
    }
}

@Composable
private fun TimeoutCounterButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        modifier = Modifier.size(48.dp),
        enabled = enabled,
        onClick = onClick,
    ) {
        CompositionLocalProvider(
            androidx.compose.material.LocalContentColor provides RuuviStationTheme.colors.accent,
            androidx.compose.material.LocalContentAlpha provides if (enabled) 1f else 0.3f,
        ) {
            content()
        }
    }
}

internal fun previousDashboardCardTimeout(seconds: Long): Long =
    DASHBOARD_CARD_TIMEOUT_STEPS.lastOrNull { it < seconds }
        ?: DASHBOARD_CARD_TIMEOUT_STEPS.first()

internal fun nextDashboardCardTimeout(seconds: Long): Long =
    DASHBOARD_CARD_TIMEOUT_STEPS.firstOrNull { it > seconds }
        ?: DASHBOARD_CARD_TIMEOUT_STEPS.last()

internal fun formatDashboardCardTimeout(seconds: Long): String =
    if (seconds < 60) {
        "${seconds}s"
    } else {
        "${seconds / 60} min"
    }

private val DASHBOARD_CARD_TIMEOUT_STEPS = listOf(
    MIN_DASHBOARD_CARD_TIMEOUT_SECONDS,
    10L,
    30L,
    60L,
    5 * 60L,
    10 * 60L,
    15 * 60L,
    30 * 60L,
    60 * 60L,
)

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
