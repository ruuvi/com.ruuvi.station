package com.ruuvi.station.network.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.network.domain.NetworkTokenInteractor

class SignedInViewModel (
    val tokenInteractor: NetworkTokenInteractor
) : ViewModel() {

    val emailObserve: LiveData<String> = MutableLiveData(tokenInteractor.getTokenInfo()?.email)
}