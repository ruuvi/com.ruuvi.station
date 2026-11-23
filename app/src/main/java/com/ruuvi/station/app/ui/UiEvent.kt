package com.ruuvi.station.app.ui

import com.ruuvi.station.util.ui.ToastDuration

sealed class UiEvent {
    data class Navigate(val route: String, val popBackStack: Boolean = false): UiEvent()
    data class NavigateNew(val route: Any, val popBackStack: Boolean = false): UiEvent()
    object NavigateUp: UiEvent()
    object Finish: UiEvent()
    data class ShowToast(val message: UiText, val duration: ToastDuration = ToastDuration.Short): UiEvent()
    data class ShowPopup(val message: UiText): UiEvent()
    data class Progress(val inProgress: Boolean, val message: UiText?): UiEvent()
}
