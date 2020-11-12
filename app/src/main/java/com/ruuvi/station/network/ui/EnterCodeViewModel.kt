package com.ruuvi.station.network.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.NetworkSignInInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor

class EnterCodeViewModel (
    val networkInteractor: RuuviNetworkInteractor,
    val networkSignInInteractor: NetworkSignInInteractor,
    val networkDataSyncInteractor: NetworkDataSyncInteractor
) : ViewModel() {

    private val errorText = MutableLiveData<String>("")
    val errorTextObserve: LiveData<String> = errorText

    private val successfullyVerified = MutableLiveData<Boolean>(false)
    val successfullyVerifiedObserve: LiveData<Boolean> = successfullyVerified

    val signedIn = networkInteractor.signedIn

    fun verifyCode(token: String) {
        networkSignInInteractor.signIn(token) { response->
            if (response.isNullOrEmpty()) {
                successfullyVerified.value = true
                networkDataSyncInteractor.syncNetworkData()
            } else {
                errorText.value = response
            }
        }
    }
}