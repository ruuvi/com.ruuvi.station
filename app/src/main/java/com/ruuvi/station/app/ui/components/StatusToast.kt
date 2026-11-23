package com.ruuvi.station.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.util.ui.showToast
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber

@Composable
fun ShowStatusToast(uiEvent: SharedFlow<UiEvent>) {
    val context = LocalContext.current

    LaunchedEffect(uiEvent) {
        uiEvent.collect { uiEvent ->
            if (uiEvent is UiEvent.ShowToast) {
                Timber.d("ShowToast $uiEvent")
                context.showToast(uiEvent.message.asString(context), uiEvent.duration)
            }
        }
    }
}
