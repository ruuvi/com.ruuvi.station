package com.ruuvi.station.network.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.network.domain.RuuviNetworkInteractor

class EnterCodeViewModel (
    val networkInteractor: RuuviNetworkInteractor
) : ViewModel() {

    private val errorText = MutableLiveData<String>("")
    val errorTextObserve: LiveData<String> = errorText

    private val successfullyVerified = MutableLiveData<Boolean>(false)
    val successfullyVerifiedObserve: LiveData<Boolean> = successfullyVerified

    val signedIn = networkInteractor.signedIn

    fun verifyCode(token: String) {
        networkInteractor.verifyUser(token) {response->
            if (response == null) {
                errorText.value = "Unknown error"
            } else {
                if (response.error.isNullOrEmpty()) {
                    successfullyVerified.value = true
                } else {
                    errorText.value = response.error
                }
            }
        }
    }
}