package com.ruuvi.station.network.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.network.data.NetworkTokenInfo
import com.ruuvi.station.network.domain.NetworkTokenInteractor
import com.ruuvi.station.network.domain.RuuviNetworkRepository

class EnterCodeViewModel (
    val networkRepository: RuuviNetworkRepository,
    val tokenInteractor: NetworkTokenInteractor
) : ViewModel() {

    private val errorText = MutableLiveData<String>("")
    val errorTextObserve: LiveData<String> = errorText

    private val successfullyVerified = MutableLiveData<Boolean>(false)
    val successfullyVerifiedObserve: LiveData<Boolean> = successfullyVerified

    fun verifyCode(token: String) {
        networkRepository.verifyUser(token) {resonse->
            if (resonse == null) {
                errorText.value = "Unknown error"
            } else {
                if (resonse.error.isNullOrEmpty() == false) {
                    errorText.value = resonse.error
                } else {
                    tokenInteractor.saveTokenInfo(
                        NetworkTokenInfo(resonse.data.email, resonse.data.accessToken))
                    successfullyVerified.value = true
                }
            }
        }
    }
}