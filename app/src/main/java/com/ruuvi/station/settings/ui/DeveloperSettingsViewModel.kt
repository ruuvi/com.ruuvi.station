package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.feature.data.Feature
import com.ruuvi.station.feature.provider.RuntimeFeatureFlagProvider
import com.ruuvi.station.network.data.response.UserVerifyResponseBody
import com.ruuvi.station.network.domain.NetworkTokenRepository
import com.ruuvi.station.network.domain.RuuviNetworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DeveloperSettingsViewModel(
    val preferencesRepository: PreferencesRepository,
    val ruuviNetworkRepository: RuuviNetworkRepository,
    private val networkTokenRepository: NetworkTokenRepository,
    val runtimeFeatureFlagProvider: RuntimeFeatureFlagProvider
): ViewModel() {

    private var _devServerEnabled = MutableStateFlow(preferencesRepository.isDevServerEnabled())
    val devServerEnabled: StateFlow<Boolean> = _devServerEnabled

    fun setDevServerEnabled(isEnabled: Boolean) {
        preferencesRepository.setDevServerEnabled(isEnabled)
        _devServerEnabled.value = preferencesRepository.isDevServerEnabled()
        ruuviNetworkRepository.reinitialize()
    }

    fun getWebViewToken(): String? {
        val tokenInfo = networkTokenRepository.getTokenInfo()
        if (tokenInfo != null) {
            val userRegisterResponse = UserVerifyResponseBody(tokenInfo.email, tokenInfo.token, false)
            return Gson().toJson(userRegisterResponse)
        } else {
            return null
        }

    }

    fun getFeatureState(feature: Feature): Boolean {
        return runtimeFeatureFlagProvider.isFeatureEnabled(feature)
    }

    fun setFeatureValue(feature: Feature, enabled: Boolean) {
        runtimeFeatureFlagProvider.setFeatureEnabled(feature, enabled)
    }
}