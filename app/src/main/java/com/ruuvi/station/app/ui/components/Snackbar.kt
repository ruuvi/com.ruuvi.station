package com.ruuvi.station.app.ui.components

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.ruuvi.station.app.ui.UiEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

@Composable
fun ShowStatusSnackbar(
    scaffoldState: ScaffoldState,
    uiEvent: SharedFlow<UiEvent>,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        uiEvent.collectLatest { uiEvent ->
            when (uiEvent) {
                is UiEvent.ShowSnackbar -> {
                    Timber.d("ShowSnackbar $uiEvent")
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = uiEvent.message.asString(context),
                        duration = uiEvent.duration
                    )
                }
                else -> {}
            }
        }
    }
}