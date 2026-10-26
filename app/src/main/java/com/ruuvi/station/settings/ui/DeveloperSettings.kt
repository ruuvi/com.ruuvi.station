package com.ruuvi.station.settings.ui

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings.LOAD_DEFAULT
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.*
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
    val marketingConsentPromptIntervalSeconds by
        viewModel.marketingConsentPromptIntervalSeconds.collectAsState()
    var marketingConsentPromptIntervalSecondsText by remember(marketingConsentPromptIntervalSeconds) {
        mutableStateOf(
            marketingConsentPromptIntervalSeconds.takeIf { it > 0L }?.toString().orEmpty()
        )
    }

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

            Spacer(modifier = Modifier.height(24.dp))
            Subtitle(text = stringResource(id = R.string.marketing_consent_prompt_interval))
            TextFieldRuuvi(
                modifier = Modifier.fillMaxWidth(),
                value = marketingConsentPromptIntervalSecondsText,
                hint = stringResource(id = R.string.marketing_consent_prompt_interval_hint),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                onValueChange = { value ->
                    if (value.all(Char::isDigit)) {
                        marketingConsentPromptIntervalSecondsText = value
                        viewModel.setMarketingConsentPromptIntervalSeconds(value.toLongOrNull() ?: 0L)
                    }
                }
            )
            Paragraph(text = stringResource(id = R.string.marketing_consent_prompt_interval_description))

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
