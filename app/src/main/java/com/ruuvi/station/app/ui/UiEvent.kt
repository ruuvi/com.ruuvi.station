package com.ruuvi.station.app.ui

sealed class UiEvent {
    data class Navigate(val route: String): UiEvent()
    object NavigateUp: UiEvent()
    data class ShowSnackbar(val message: UiText): UiEvent()
    data class ShowPopup(val message: UiText): UiEvent()
}