package com.ruuvi.station.network.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.util.extensions.diffGreaterThan
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class SubscriptionInfoSyncInteractor (
    val preferencesRepository: PreferencesRepository,
    val networkInteractor: RuuviNetworkInteractor
) {
    fun syncSubscriptionInfo() {
        val lastRefresh = preferencesRepository.getSubscriptionRefreshDate()

        val timeToRefreshInfo = Date(lastRefresh).diffGreaterThan(refreshInterval)

        if (timeToRefreshInfo) {
            Timber.d("syncSubscriptionInfo")
            val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
                Timber.e(throwable,"syncSubscriptionInfo exception")
            }

            CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
                networkInteractor.getSubscription { response ->
                    if (response?.isSuccess() == true) {
                        val subscription = response.data?.subscriptions?.firstOrNull { it.isActive }

                        if (subscription != null) {
                            preferencesRepository.setSubscriptionMaxSharesPerSensor(subscription.maxSharesPerSensor)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val refreshInterval = 1000L * 3600 * 24 // 24h
    }
}