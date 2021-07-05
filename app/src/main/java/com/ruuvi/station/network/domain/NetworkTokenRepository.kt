package com.ruuvi.station.network.domain

import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.network.data.NetworkTokenInfo

class NetworkTokenRepository (
    private val preferences: Preferences
) {
    fun saveTokenInfo(tokenInfo: NetworkTokenInfo) {
        setNetworkTokenInfo(tokenInfo)
    }

    fun getTokenInfo(): NetworkTokenInfo? = getNetworkTokenInfo()

    fun signOut() {
        saveTokenInfo(NetworkTokenInfo("", ""))
        preferences.lastSyncDate = Long.MIN_VALUE
    }

    private fun getNetworkTokenInfo(): NetworkTokenInfo? {
        if (preferences.networkEmail.isEmpty() || preferences.networkToken.isEmpty()) {
            return null
        } else {
            return NetworkTokenInfo(preferences.networkEmail, preferences.networkToken)
        }
    }

    private fun setNetworkTokenInfo(tokenInfo: NetworkTokenInfo) {
        preferences.networkEmail = tokenInfo.email
        preferences.networkToken = tokenInfo.token
    }
}