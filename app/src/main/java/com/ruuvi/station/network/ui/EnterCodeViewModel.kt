package com.ruuvi.station.network.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.NetworkSignInInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import kotlinx.coroutines.launch

class EnterCodeViewModel (
    val networkInteractor: RuuviNetworkInteractor,
    val networkSignInInteractor: NetworkSignInInteractor,
    val networkDataSyncInteractor: NetworkDataSyncInteractor
) : ViewModel() {

    private val errorText = MutableLiveData<String>("")
    val errorTextObserve: LiveData<String> = errorText

    private val successfullyVerified = MutableLiveData<Boolean>(false)
    val successfullyVerifiedObserve: LiveData<Boolean> = successfullyVerified

    private val requestInProcess = MutableLiveData<Boolean>(false)
    val requestInProcessObserve: LiveData<Boolean> = requestInProcess

    fun  isSignedIn() = networkInteractor.signedIn

    fun getUserEmail() = networkInteractor.getEmail()

    fun verifyCode(token: String) {
        requestInProcess.value = true
        errorText.value = ""
        networkSignInInteractor.signIn(token) { response->
            if (response.isNullOrEmpty()) {
                viewModelScope.launch {
                    val syncJob = networkDataSyncInteractor.syncNetworkData()
                    syncJob.join()
                    requestInProcess.value = false
                    successfullyVerified.value = true
                }
            } else {
                errorText.value = response
                requestInProcess.value = false
            }
        }
    }
}