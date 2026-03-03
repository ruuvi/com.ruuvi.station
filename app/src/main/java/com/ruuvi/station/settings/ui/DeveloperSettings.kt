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
import androidx.compose.foundation.layout.height
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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

    PageSurfaceWithPadding {
        Column() {
//            FeatureSwitch(
//                feature = FeatureFlag.VISIBLE_MEASUREMENTS,
//                checked = viewModel::getFeatureState,
//                onCheckedChange = viewModel::setFeatureValue
//            )

//            FeatureSwitch(
//                feature = FeatureFlag.NEW_SENSOR_CARD,
//                checked = viewModel::getFeatureState,
//                onCheckedChange = viewModel::setFeatureValue
//            )
            SwitchIndicatorRuuvi(
                text = "Use web version of share",
                checked = useWebShare,
                onCheckedChange = viewModel::setUseWebShare
            )

            SwitchIndicatorRuuvi(
                text = stringResource(id = R.string.use_dev_server),
                checked = devServerEnabled,
                onCheckedChange = viewModel::setDevServerEnabled
            )
            Paragraph(text = stringResource(id = R.string.use_dev_server_description))

            Spacer(modifier = Modifier.height(48.dp))
            Subtitle("Debug info")
            val scale = LocalDensity.current.fontScale
            Paragraph(text = "Font scaling ${scale * 100}%")
            Spacer(modifier = Modifier.height(48.dp))
            RuuviButton(text = "Disable dev mode") {
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
