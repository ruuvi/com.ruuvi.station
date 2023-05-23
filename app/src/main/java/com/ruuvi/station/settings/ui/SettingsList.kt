package com.ruuvi.station.settings.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.components.DividerRuuvi
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.util.BackgroundScanModes
import java.util.*

@Composable
fun SettingsList(
    scaffoldState: ScaffoldState,
    onNavigate: (UiEvent.Navigate) -> Unit,
    viewModel: AppSettingsListViewModel
) {
    val context = LocalContext.current
    BackHandler() {
        (context as Activity).finish()
    }

    var intervalText = ""
    if (viewModel.getBackgroundScanMode() != BackgroundScanModes.DISABLED) {
        val bgScanInterval = viewModel.getBackgroundScanInterval()
        val min = bgScanInterval / 60
        val sec = bgScanInterval - min * 60
        if (min > 0) intervalText += min.toString() + " " + stringResource(R.string.min) + " "
        if (sec > 0) intervalText += sec.toString() + " " + stringResource(R.string.sec)
    } else {
        intervalText = stringResource(id = R.string.alert_subtitle_off)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (Build.VERSION.SDK_INT >= TIRAMISU) {
            item {
                SettingsElement(
                    name = stringResource(id = R.string.settings_language),
                    value = Locale.getDefault().displayLanguage,
                    onClick = {
                        val intent = Intent(android.provider.Settings.ACTION_APP_LOCALE_SETTINGS)
                        intent.data = Uri.parse("package:${context.packageName}")
                        context.startActivity(intent)
                    }
                )
            }
        }

        if (Build.VERSION.SDK_INT >= O) {
            item {
                SettingsElement(
                    name = stringResource(id = R.string.settings_alert_notifications),
                    value = null,
                    onClick = {
                        onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.ALERT_NOTIFICATIONS))
                    }
                )
            }
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_appearance),
                value = null,
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.APPEARANCE)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_background_scan),
                value = intervalText,
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.BACKGROUNDSCAN)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_temperature),
                value = stringResource(id = viewModel.getTemperatureUnit().unit),
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.TEMPERATURE)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_humidity),
                value = stringResource(id = viewModel.getHumidityUnit().unit),
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.HUMIDITY)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_pressure),
                value = stringResource(id = viewModel.getPressureUnit().unit),
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.PRESSURE)) }
            )
        }

        if (viewModel.shouldShowCloudMode()) {
            item {
                SettingsElement(
                    name = stringResource(id = R.string.ruuvi_cloud),
                    value = null,
                    onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.CLOUD)) }
                )
            }
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_chart),
                value = null,
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.CHARTS)) }
            )
        }

        item {
            SettingsElement(
                name = stringResource(id = R.string.settings_data_forwarding),
                value = null,
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.DATAFORWARDING)) }
            )
        }
    }
}

@Composable
fun SettingsElement(
    name: String,
    fixedHeight: Boolean = true,
    value: String? = null,
    description: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        val modifier = if (fixedHeight) {
            Modifier.height(RuuviStationTheme.dimensions.settingsListHeight)
        } else {
            Modifier
        }

        ConstraintLayout(modifier = modifier
            .fillMaxWidth()
            .padding(
                vertical = RuuviStationTheme.dimensions.screenPadding,
                horizontal = RuuviStationTheme.dimensions.screenPadding
            )
            .clickable(role = Role.Button) { onClick.invoke() }
        ) {
            val (caption, descElement, arrow) = createRefs()

            Column(modifier = Modifier
                .constrainAs(caption) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.absoluteLeft)
                    end.linkTo(descElement.start)
                    width = Dimension.fillToConstraints
                    height = Dimension.wrapContent
                },
            ) {

                Text(
                    style = RuuviStationTheme.typography.subtitle,
                    text = name,
                    textAlign = TextAlign.Left,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!description.isNullOrEmpty()) {
                    Text(
                        style = RuuviStationTheme.typography.subtitle.copy(
                            color = RuuviStationTheme.colors.secondaryTextColor,
                            fontFamily = ruuviStationFonts.mulishRegular),
                        text = description,
                        textAlign = TextAlign.Left
                    )
                }
            }

            Column(modifier = Modifier
                .constrainAs(descElement) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(arrow.start)
                },
            ) {
                if (!value.isNullOrEmpty()) {
                    Text(
                        style = RuuviStationTheme.typography.paragraph,
                        textAlign = TextAlign.End,
                        text = value,
                        maxLines = 1,
                        overflow = TextOverflow.Visible
                    )
                }
            }

            Image(
                painter = painterResource(id = R.drawable.arrow_forward_16),
                contentDescription = "",
                modifier = Modifier
                    .padding(start = RuuviStationTheme.dimensions.mediumPlus)
                    .constrainAs(arrow) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)
                    }
            )
        }
        DividerRuuvi()
    }
}