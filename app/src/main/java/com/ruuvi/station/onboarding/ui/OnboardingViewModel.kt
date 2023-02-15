package com.ruuvi.station.onboarding.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.network.domain.RuuviNetworkInteractor

class OnboardingViewModel(
    private val networkInteractor: RuuviNetworkInteractor
): ViewModel() {
    val signedIn = networkInteractor.signedIn
}