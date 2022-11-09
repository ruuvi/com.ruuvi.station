package com.ruuvi.station.network.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.NetworkTokenRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class MyAccountViewModel(
    private val networkDataSyncInteractor: NetworkDataSyncInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val tokenRepository: NetworkTokenRepository,
    private val networkInteractor: RuuviNetworkInteractor
): ViewModel() {

    val userEmail = preferencesRepository.getUserEmailLiveData()

    private val _events = MutableSharedFlow<MyAccountEvent>()
    val events: SharedFlow<MyAccountEvent> = _events

    fun signOut() {
        networkDataSyncInteractor.stopSync()
        tokenRepository.signOut {
            sendEvent(MyAccountEvent.CloseActivity)
        }
    }

    fun removeAccount() {
        viewModelScope.launch {
            sendEvent(MyAccountEvent.Loading(true))
            networkInteractor.requestDeleteAccount {
                sendEvent(MyAccountEvent.Loading(false))
                if (it?.isSuccess() == true) {
                    sendEvent(MyAccountEvent.RequestRegistered)
                }
            }
        }
    }

    fun sendEvent(event: MyAccountEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }


}

sealed class MyAccountEvent {
    object CloseActivity: MyAccountEvent()
    class Loading(val isLoading: Boolean): MyAccountEvent()
    object RequestRegistered: MyAccountEvent()
}