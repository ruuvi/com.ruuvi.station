package com.ruuvi.station.network.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.network.domain.NetworkTokenRepository

class SignOutViewModel (
    val tokenRepository: NetworkTokenRepository
) : ViewModel() {

    fun signOut() {
        tokenRepository.signOut()
    }
}