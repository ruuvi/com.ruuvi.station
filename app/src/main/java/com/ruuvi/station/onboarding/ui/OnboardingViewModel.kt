package com.ruuvi.station.onboarding.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.firebase.domain.FirebaseInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor

class OnboardingViewModel(
    private val networkInteractor: RuuviNetworkInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val firebaseInteractor: FirebaseInteractor
): ViewModel() {
    val signedIn = networkInteractor.signedIn

    val firstStart = preferencesRepository.isFirstStart()

    fun onboardingFinished(firebaseConsent: Boolean) {
        preferencesRepository.setFirstStart(false)
        preferencesRepository.setAcceptTerms(true)
        preferencesRepository.setFirebaseConsent(firebaseConsent)
        firebaseInteractor.saveUserConsent()
    }
}