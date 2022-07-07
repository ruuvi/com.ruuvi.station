package com.ruuvi.station.util.extensions

import androidx.navigation.NavController
import com.ruuvi.station.app.ui.UiEvent

fun NavController.navigate(event: UiEvent.Navigate) {
    this.navigate(event.route)
}