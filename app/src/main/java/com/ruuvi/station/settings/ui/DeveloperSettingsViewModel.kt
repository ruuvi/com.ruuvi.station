package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.data.response.UserVerifyResponseBody
import com.ruuvi.station.network.domain.NetworkTokenRepository
import com.ruuvi.station.network.domain.RuuviNetworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DeveloperSettingsViewModel(
    val preferencesRepository: PreferencesRepository,
    val ruuviNetworkRepository: RuuviNetworkRepository,
    private val networkTokenRepository: NetworkTokenRepository,
): ViewModel() {

    private var _devServerEnabled = MutableStateFlow(preferencesRepository.isDevServerEnabled())
    val devServerEnabled: StateFlow<Boolean> = _devServerEnabled

    private var _newChartsUiEnabled = MutableStateFlow(preferencesRepository.isNewChartsUI())
    val newChartsUiEnabled: StateFlow<Boolean> = _newChartsUiEnabled

    fun setDevServerEnabled(isEnabled: Boolean) {
        preferencesRepository.setDevServerEnabled(isEnabled)
        _devServerEnabled.value = preferencesRepository.isDevServerEnabled()
        ruuviNetworkRepository.reinitialize()
    }
    fun setNewChartsUi(isEnabled: Boolean) {
        preferencesRepository.setNewChartsUI(isEnabled)
        _newChartsUiEnabled.value = preferencesRepository.isNewChartsUI()
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
}