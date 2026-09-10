package com.ruuvi.station.network.domain

import com.ruuvi.station.app.locale.LocaleInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.data.response.MarketingConsentResponseBody
import com.ruuvi.station.network.data.response.MarketingConsentStatus
import com.ruuvi.station.feature.data.FeatureFlag
import com.ruuvi.station.feature.domain.RuntimeBehavior

class MarketingConsentInteractor(
    private val networkInteractor: RuuviNetworkInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val localeInteractor: LocaleInteractor,
    private val runtimeBehavior: RuntimeBehavior
) {
    fun isEnabled(): Boolean = runtimeBehavior.isFeatureEnabled(FeatureFlag.MARKETING_CONSENT)

    suspend fun refresh(): MarketingConsentResponseBody? {
        if (!isEnabled()) return null
        val response = networkInteractor.getMarketingConsent()
        return response?.data?.takeIf { response.isSuccess() }?.also(::store)
    }

    suspend fun update(consent: Boolean): MarketingConsentResponseBody? {
        if (!isEnabled()) return null
        val response = networkInteractor.setMarketingConsent(
            consent = consent,
            language = localeInteractor.getCurrentLocaleLanguage()
        )
        return response?.data?.takeIf { response.isSuccess() }?.also(::store)
    }

    private fun store(consent: MarketingConsentResponseBody) {
        preferencesRepository.setNetworkSetting(
            NetworkSettingNames.MARKETING_PERMISSION,
            (consent.consentStatus == MarketingConsentStatus.SUBSCRIBED).toString(),
            System.currentTimeMillis() / 1000
        )
    }
}
