package com.ruuvi.station.network.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.network.data.request.UserRegisterRequest
import com.ruuvi.station.network.domain.RuuviNetworkInteractor

class EmailEnterViewModel (
    val networkInteractor: RuuviNetworkInteractor
) : ViewModel() {

    private val errorText = MutableLiveData<String>("")
    val errorTextObserve: LiveData<String> = errorText

    private val successfullyRegistered = MutableLiveData<Boolean>(false)
    val successfullyRegisteredObserve: LiveData<Boolean> = successfullyRegistered

    private val alreadyLoggedIn = MutableLiveData<Boolean>(networkInteractor.signedIn)
    val alreadyLoggedInObserve: LiveData<Boolean> = alreadyLoggedIn

    fun submitEmail(email: String) {
        networkInteractor.registerUser(UserRegisterRequest(email = email)) {
            if (it == null) {
                errorText.value = "Unknown error"
            } else {
                if (it.error.isNullOrEmpty() == false) {
                    errorText.value = it.error
                } else {
                    successfullyRegistered.value = true
                }
            }
        }
    }
}