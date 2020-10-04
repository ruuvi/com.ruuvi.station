package com.ruuvi.station.network.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.data.NetworkTokenInfo

class NetworkTokenInteractor (
    private val preferencesRepository: PreferencesRepository
) {
    fun saveTokenInfo(tokenInfo: NetworkTokenInfo) {
        preferencesRepository.setNetworkTokenInfo(tokenInfo)
    }

    fun getTokenInfo(): NetworkTokenInfo? = preferencesRepository.getNetworkTokenInfo()

    fun signOut() {
        saveTokenInfo(NetworkTokenInfo("", ""))
    }
}