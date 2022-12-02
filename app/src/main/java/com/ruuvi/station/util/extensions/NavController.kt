package com.ruuvi.station.util.extensions

import androidx.navigation.NavController
import com.ruuvi.station.app.ui.UiEvent
import timber.log.Timber

fun NavController.navigate(event: UiEvent.Navigate) {
    if (event.popBackStack) this.popBackStack()
    this.navigate(event.route)
    Timber.d("Navigate to ${event.route} PopBackStack = ${event.popBackStack}")
}