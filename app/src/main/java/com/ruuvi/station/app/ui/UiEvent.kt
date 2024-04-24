package com.ruuvi.station.app.ui

import androidx.compose.material.SnackbarDuration

sealed class UiEvent {
    data class Navigate(val route: String, val popBackStack: Boolean = false): UiEvent()
    object NavigateUp: UiEvent()
    object Finish: UiEvent()
    data class ShowSnackbar(val message: UiText, val duration: SnackbarDuration = SnackbarDuration.Short): UiEvent()
    data class ShowPopup(val message: UiText): UiEvent()
    data class Progress(val inProgress: Boolean, val message: UiText?): UiEvent()
}