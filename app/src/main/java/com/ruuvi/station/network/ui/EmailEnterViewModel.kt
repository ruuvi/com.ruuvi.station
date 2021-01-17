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

    fun submitEmail(email: String) {
        networkInteractor.registerUser(UserRegisterRequest(email = email)) {
            if (it == null) {
                //todo localize
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

    fun successfullyRegisteredFinished() {
        successfullyRegistered.value = false
    }
}