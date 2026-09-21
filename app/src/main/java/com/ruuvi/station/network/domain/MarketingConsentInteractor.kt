package com.ruuvi.station.network.domain

import com.ruuvi.station.app.locale.LocaleInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.data.response.MarketingConsentResponseBody
import com.ruuvi.station.feature.data.FeatureFlag
import com.ruuvi.station.feature.domain.RuntimeBehavior
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MarketingConsentInteractor(
    private val networkInteractor: RuuviNetworkInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val localeInteractor: LocaleInteractor,
    private val runtimeBehavior: RuntimeBehavior
) {
    private val requestMutex = Mutex()

    fun isEnabled(): Boolean = runtimeBehavior.isFeatureEnabled(FeatureFlag.MARKETING_CONSENT)

    suspend fun refresh(): MarketingConsentResponseBody? = requestMutex.withLock {
        if (!isEnabled()) return@withLock null
        val response = networkInteractor.getMarketingConsent()
        response?.data?.takeIf { response.isSuccess() }?.also(::store)
    }

    suspend fun update(consent: Boolean): MarketingConsentResponseBody? = requestMutex.withLock {
        if (!isEnabled()) return@withLock null
        val response = networkInteractor.setMarketingConsent(
            consent = consent,
            language = localeInteractor.getCurrentLocaleLanguage()
        )
        response?.data?.takeIf { response.isSuccess() }?.also(::store)
    }

    private fun store(consent: MarketingConsentResponseBody) {
        preferencesRepository.setMarketingConsent(consent.consent)
    }
}
