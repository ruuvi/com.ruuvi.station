package com.ruuvi.station.network.domain

import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.network.data.NetworkTokenInfo

class NetworkTokenRepository (
    private val preferences: Preferences
) {
    fun saveTokenInfo(tokenInfo: NetworkTokenInfo) {
        setNetworkTokenInfo(tokenInfo)
    }

    fun clearTokenInfo() {
        saveTokenInfo(NetworkTokenInfo("", ""))
        preferences.lastSyncDate = Long.MIN_VALUE
    }

    fun getTokenInfo(): NetworkTokenInfo? = getNetworkTokenInfo()

    private fun getNetworkTokenInfo(): NetworkTokenInfo? {
        return if (preferences.networkEmail.isEmpty() || preferences.networkToken.isEmpty()) {
            null
        } else {
            NetworkTokenInfo(preferences.networkEmail, preferences.networkToken)
        }
    }

    private fun setNetworkTokenInfo(tokenInfo: NetworkTokenInfo) {
        preferences.networkEmail = tokenInfo.email
        preferences.networkToken = tokenInfo.token
    }

    fun signedIn() = getTokenInfo() != null
}