package com.ruuvi.station.network.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.data.response.GetSubscriptionResponse
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.NetworkSignInInteractor
import com.ruuvi.station.network.domain.MarketingConsentInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.network.data.response.MarketingConsentResponseBody
import com.ruuvi.station.network.data.response.MarketingConsentStatus
import com.ruuvi.station.settings.domain.AppSettingsInteractor
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
    private val networkSignInInteractor: NetworkSignInInteractor,
    private val appSettingsInteractor: AppSettingsInteractor,
    private val marketingConsentInteractor: MarketingConsentInteractor
): ViewModel() {

    val userEmail = preferencesRepository.getUserEmailLiveData()

    private val _events = MutableSharedFlow<MyAccountEvent>()
    val events: SharedFlow<MyAccountEvent> = _events

    private val _subscription = MutableStateFlow<Subscription?>(null)
    val subscription: StateFlow<Subscription?> = _subscription

    private val _marketingConsent = MutableStateFlow(
        MarketingConsentUiState(
            checked = appSettingsInteractor.getMarketingPermission(),
            isVisible = marketingConsentInteractor.isEnabled()
        )
    )
    val marketingConsent: StateFlow<MarketingConsentUiState> = _marketingConsent

    private val _tokens = MutableStateFlow<List<Pair<Long,String>>?>(null)
    val tokens: StateFlow<List<Pair<Long,String>>?> = _tokens

    init {
        Timber.d("init MyAccountViewModel")
        if (BuildConfig.DEBUG) {
            getSubscriptionInfo()
            getRegisteredTokens()
        }
        refreshMarketingConsent()
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

    fun setMarketingPermission(isEnabled: Boolean) {
        val previousState = _marketingConsent.value
        if (!previousState.isToggleEnabled) return

        _marketingConsent.value = previousState.copy(
            checked = isEnabled,
            isToggleEnabled = false
        )

        viewModelScope.launch {
            sendEvent(MyAccountEvent.Loading(true))
            var showUnconfirmedDialog = false
            try {
                val consent = marketingConsentInteractor.update(isEnabled)
                if (consent == null) {
                    _marketingConsent.value = previousState
                } else {
                    applyMarketingConsent(consent)
                    if (isEnabled && consent.consentStatus == MarketingConsentStatus.UNCONFIRMED) {
                        showUnconfirmedDialog = true
                    }
                }
            } catch (exception: Exception) {
                Timber.e(exception, "Unable to update marketing consent")
                _marketingConsent.value = previousState
            } finally {
                sendEvent(MyAccountEvent.Loading(false))
            }
            if (showUnconfirmedDialog) {
                sendEvent(MyAccountEvent.MarketingConsentUnconfirmed)
            }
        }
    }

    private fun refreshMarketingConsent() {
        viewModelScope.launch {
            try {
                marketingConsentInteractor.refresh()?.let(::applyMarketingConsent)
            } catch (exception: Exception) {
                // Marketing consent is supplementary account data. Its failure must not
                // prevent sign-in or make the rest of the account page unavailable.
                Timber.e(exception, "Unable to refresh marketing consent")
            }
        }
    }

    private fun applyMarketingConsent(consent: MarketingConsentResponseBody) {
        _marketingConsent.value = MarketingConsentUiState(
            checked = consent.consentStatus == MarketingConsentStatus.SUBSCRIBED,
            status = consent.consentStatus,
            isToggleEnabled = consent.consentStatus.isToggleEnabled,
            isVisible = marketingConsentInteractor.isEnabled()
        )
    }
}

sealed class MyAccountEvent {
    object CloseActivity: MyAccountEvent()
    class Loading(val isLoading: Boolean): MyAccountEvent()
    object RequestRegistered: MyAccountEvent()
    object MarketingConsentUnconfirmed: MyAccountEvent()
}

data class MarketingConsentUiState(
    val checked: Boolean,
    val status: MarketingConsentStatus? = null,
    val isToggleEnabled: Boolean = false,
    val isVisible: Boolean = false
) {
    val showConfirmationMessage: Boolean
        get() = status == MarketingConsentStatus.UNCONFIRMED
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
