package com.ruuvi.station.widgets.ui

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.components.RuuviButton
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun WidgetConfigTopAppBar(
    viewModel: ICloudWidgetViewModel,
    title: String
) {
    val context = LocalContext.current as Activity
    val readyToBeSaved by viewModel.canBeSaved.observeAsState()
    val systemUiController = rememberSystemUiController()

    TopAppBar(
        title = {
            Text(text = title, style = RuuviStationTheme.typography.topBarText)
        },
        navigationIcon = {
            IconButton(onClick = {
                context.onBackPressed()
            }) {
                Icon(Icons.Default.ArrowBack, stringResource(id = R.string.back))
            }
        },
        backgroundColor = RuuviStationTheme.colors.topBar,
        contentColor = RuuviStationTheme.colors.topBarText,
        elevation = 0.dp,
        actions = {
            if (readyToBeSaved == true) {
                TextButton(
                    onClick = { viewModel.save() }
                ) {
                    Text(
                        color = RuuviStationTheme.colors.topBarText,
                        style = RuuviStationTheme.typography.topBarText,
                        text = stringResource(id = R.string.done)
                    )
                }
            }
        }
    )

    val systemBarsColor = RuuviStationTheme.colors.systemBars
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = systemBarsColor,
            darkIcons = false
        )
    }
}

@Composable
fun LogInFirstScreen() {
    Column() {
        Paragraph(
            text = stringResource(id = R.string.widgets_sign_in_first),
            modifier = Modifier.padding(RuuviStationTheme.dimensions.screenPadding)
        )
        Paragraph(
            text = stringResource(id = R.string.widgets_gateway_only),
            modifier = Modifier.padding(RuuviStationTheme.dimensions.screenPadding)
        )
    }
}

@Composable
fun AddSensorsFirstScreen() {
    Column() {
        Paragraph(
            text = stringResource(id = R.string.widgets_add_sensors_first),
            modifier = Modifier.padding(RuuviStationTheme.dimensions.screenPadding)
        )
    }
}

@Composable
fun EnableBackgroundService(
    bgScanInterval: Int,
    enableBackgroundService: () -> Unit
) {
    var intervalText = ""
    val min = bgScanInterval / 60
    val sec = bgScanInterval - min * 60
    if (min > 0) intervalText += min.toString() + " " + stringResource(R.string.min) + " "
    if (sec > 0) intervalText += sec.toString() + " " + stringResource(R.string.sec)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Paragraph(
            text = stringResource(id = R.string.widgets_enable_background_service, intervalText),
            modifier = Modifier.padding(RuuviStationTheme.dimensions.screenPadding)
        )
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.mediumPlus))
        RuuviButton(text = "Enable background service") {
            enableBackgroundService.invoke()
        }
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.mediumPlus))
    }
}

interface ICloudWidgetViewModel {
    val canBeSaved: LiveData<Boolean>
    val userLoggedIn: LiveData<Boolean>
    val userHasCloudSensors: LiveData<Boolean>
    fun save()
}
