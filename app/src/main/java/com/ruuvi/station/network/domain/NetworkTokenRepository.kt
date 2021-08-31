package com.ruuvi.station.network.domain

import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.data.NetworkTokenInfo
import kotlinx.coroutines.*

class NetworkTokenRepository (
    private val preferences: Preferences,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val tagRepository: TagRepository
) {
    fun saveTokenInfo(tokenInfo: NetworkTokenInfo) {
        setNetworkTokenInfo(tokenInfo)
    }

    fun getTokenInfo(): NetworkTokenInfo? = getNetworkTokenInfo()

    fun signOut(finished: ()->Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            saveTokenInfo(NetworkTokenInfo("", ""))
            preferences.lastSyncDate = Long.MIN_VALUE

            val sensors = sensorSettingsRepository.getSensorSettings()
            for (sensor in sensors) {
                if (sensor.networkSensor) {
                    tagRepository.deleteSensorAndRelatives(sensor.id)
                }
            }
            withContext(Dispatchers.Main){
                finished()
            }
        }
    }

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
}