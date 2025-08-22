package com.ruuvi.station.network.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.data.response.GetSubscriptionResponse
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.NetworkSignInInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class MyAccountViewModel(
    private val networkDataSyncInteractor: NetworkDataSyncInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val networkInteractor: RuuviNetworkInteractor,
    private val networkSignInInteractor: NetworkSignInInteractor
): ViewModel() {

    val userEmail = preferencesRepository.getUserEmailLiveData()

    private val _events = MutableSharedFlow<MyAccountEvent>()
    val events: SharedFlow<MyAccountEvent> = _events

    private val _subscription = MutableStateFlow<Subscription?>(null)
    val subscription: StateFlow<Subscription?> = _subscription

    private val _tokens = MutableStateFlow<List<Pair<Long,String>>?>(null)
    val tokens: StateFlow<List<Pair<Long,String>>?> = _tokens

    init {
        Timber.d("init MyAccountViewModel")
        if (BuildConfig.DEBUG) {
            getSubscriptionInfo()
            getRegisteredTokens()
        }
    }

    fun signOut() {
        networkSignInInteractor.signOut {
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

    fun getSubscriptionInfo() {
        viewModelScope.launch {
            networkInteractor.getSubscription { response ->
                _subscription.value = Subscription.getFromResponse(response)
            }
        }
    }

    fun getRegisteredTokens() {
        viewModelScope.launch {
            try {
                val response = networkInteractor.getPushList()
                if (response != null && response.isSuccess()) {
                    _tokens.value = response.data?.tokens?.map { Pair(it.id, it.name) }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}

sealed class MyAccountEvent {
    object CloseActivity: MyAccountEvent()
    class Loading(val isLoading: Boolean): MyAccountEvent()
    object RequestRegistered: MyAccountEvent()
}

data class Subscription (
    val name: String,
    val endTime: Date?,
    val maxClaims: Int,
    val maxShares: Int,
    val maxSharesPerSensor: Int,
) {
    companion object {
        fun getFromResponse(response: GetSubscriptionResponse?): Subscription? {
            val activeSubscription = response?.data?.subscriptions?.firstOrNull { it.isActive }
            return activeSubscription?.let {
                Subscription(
                    name = it.subscriptionName,
                    endTime = it.endTime?.let { endTime -> Date(endTime * 1000) },
                    maxClaims = it.maxClaims,
                    maxShares = it.maxShares,
                    maxSharesPerSensor = it.maxSharesPerSensor
                )
            }
        }
    }
}