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

    private val requestInProcess = MutableLiveData<Boolean>(false)
    val requestInProcessObserve: LiveData<Boolean> = requestInProcess

    fun submitEmail(email: String) {
        requestInProcess.value = true
        errorText.value = ""
        networkInteractor.registerUser(UserRegisterRequest(email = email)) {
            if (it == null) {
                //TODO LOCALIZE
                errorText.value = "Unknown error"
            } else {
                if (it.error.isNullOrEmpty() == false) {
                    errorText.value = it.error
                } else {
                    successfullyRegistered.value = true
                }
            }
            requestInProcess.value = false
        }
    }

    fun successfullyRegisteredFinished() {
        successfullyRegistered.value = false
    }
}