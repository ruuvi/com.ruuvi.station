package com.ruuvi.station.util.extensions

import androidx.navigation.NavController
import com.ruuvi.station.app.ui.UiEvent
import timber.log.Timber

fun NavController.navigate(event: UiEvent.Navigate) {
    this.navigate(event.route)
    Timber.d("Navigate to ${event.route}")
}

fun NavController.navigateAndPopBackStack(event: UiEvent.Navigate) {
    this.popBackStack()
    this.navigate(event.route)
    Timber.d("Navigate to ${event.route}. PopBackStack")
}