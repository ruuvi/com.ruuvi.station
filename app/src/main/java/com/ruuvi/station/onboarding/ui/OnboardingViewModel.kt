package com.ruuvi.station.onboarding.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor

class OnboardingViewModel(
    private val networkInteractor: RuuviNetworkInteractor,
    private val preferencesRepository: PreferencesRepository
): ViewModel() {
    val signedIn = networkInteractor.signedIn

    fun onboardingFinished() {
        preferencesRepository.setFirstStart(false)
    }
}