package com.ruuvi.station.network.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.network.domain.NetworkTokenInteractor

class SignOutViewModel (
    val tokenInteractor: NetworkTokenInteractor
) : ViewModel() {

    fun signOut() {
        tokenInteractor.signOut()
    }
}