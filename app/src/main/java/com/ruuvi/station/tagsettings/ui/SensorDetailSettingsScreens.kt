package com.ruuvi.station.tagsettings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.ruuvi.station.alarm.ui.AlarmItemsViewModel
import com.ruuvi.station.alarm.ui.AlarmsGroup
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun SensorSettingsRootScreen(
    scaffoldState: ScaffoldState,
    viewModel: TagSettingsViewModel,
    onNavigate: (String) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        viewModel.checkIfSensorShared()
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                viewModel.getTagInfo()
                delay(1_000)
            }
        }
    }

    SensorSettings(
        scaffoldState = scaffoldState,
        onNavigate = onNavigate,
        viewModel = viewModel,
    )
}

@Composable
fun SensorAlertsScreen(
    scaffoldState: ScaffoldState,
    viewModel: AlarmItemsViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AlarmsGroup(
            scaffoldState = scaffoldState,
            viewModel = viewModel,
            showTitle = false,
        )
    }
}
