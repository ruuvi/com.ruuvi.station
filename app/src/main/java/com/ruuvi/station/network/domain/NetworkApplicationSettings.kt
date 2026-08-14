package com.ruuvi.station.network.domain

import com.ruuvi.station.app.locale.LocaleInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.data.response.NetworkUserSettings
import com.ruuvi.station.units.domain.UnitsConverter
import timber.log.Timber

class NetworkApplicationSettings (
    private val tokenRepository: NetworkTokenRepository,
    private val networkRepository: RuuviNetworkRepository,
    private val networkInteractor: RuuviNetworkInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val unitsConverter: UnitsConverter,
    private val localeInteractor: LocaleInteractor
    ) {

    private fun getToken() = tokenRepository.getTokenInfo()

    suspend fun updateSettingsFromNetwork() {
        getToken()?.token?.let { token ->
            val response = networkRepository.getUserSettings(token)
            if (response?.data != null && response.isSuccess()) {
                val cloudSettings = response.data.settings
                if (cloudSettings.isEmpty()) {
                    syncAllLocalSettingsToCloud()
                    return
                }
                syncLocalAndCloudSettings(cloudSettings)
            }
        }
    }

    private fun syncLocalAndCloudSettings(settings: NetworkUserSettings) {
        NetworkSettingNames.TRACKED_SETTINGS.forEach { settingName ->
            val cloudValue = settings.valueFor(settingName)
            val cloudTimestamp = settings.timestampFor(settingName)
            val localTimestamp = preferencesRepository.getNetworkSettingLastUpdated(settingName)

            when {
                cloudValue == null -> {
                    if (localTimestamp > 0L) {
                        pushLocalSettingToCloud(settingName, localTimestamp)
                    }
                }
                cloudTimestamp > localTimestamp -> {
                    applyCloudSetting(settingName, settings, cloudTimestamp)
                }
                localTimestamp > cloudTimestamp -> {
                    pushLocalSettingToCloud(settingName, localTimestamp)
                }
                cloudTimestamp == 0L -> {
                    val currentTimestamp = System.currentTimeMillis() / 1000
                    applyCloudSetting(settingName, settings, currentTimestamp)
                }
            }
        }
    }

    private fun syncAllLocalSettingsToCloud() {
        NetworkSettingNames.TRACKED_SETTINGS.forEach { settingName ->
            val localTimestamp = ensureLocalSettingTimestamp(settingName)
            pushLocalSettingToCloud(settingName, localTimestamp)
        }
    }

    private fun applyCloudSetting(settingName: String, settings: NetworkUserSettings, timestamp: Long) {
        preferencesRepository.setNetworkSetting(settingName, settings.valueFor(settingName), timestamp)
    }

    private fun pushLocalSettingToCloud(settingName: String, timestamp: Long) {
        if (!networkInteractor.signedIn) return
        val value = localSettingValue(settingName) ?: return
        networkInteractor.updateUserSetting(settingName, value, timestamp)
    }

    private fun localSettingValue(settingName: String): String? {
        return when (settingName) {
            NetworkSettingNames.PROFILE_LANGUAGE_CODE -> localeInteractor.getCurrentLocaleLanguage()
            else -> preferencesRepository.getNetworkSetting(settingName)
        }
    }

    private fun ensureLocalSettingTimestamp(settingName: String): Long {
        val localTimestamp = preferencesRepository.getNetworkSettingLastUpdated(settingName)
        return if (localTimestamp > 0L) {
            localTimestamp
        } else {
            markLocalSettingUpdated(settingName)
        }
    }

    private fun markLocalSettingUpdated(settingName: String): Long {
        val timestamp = System.currentTimeMillis() / 1000
        val value = localSettingValue(settingName)
        preferencesRepository.setNetworkSetting(settingName, value, timestamp)
        return timestamp
    }


    fun updateNetworkSetting(settingName: String) {
        val timestamp = markLocalSettingUpdated(settingName)
        if (networkInteractor.signedIn) {
            val value = preferencesRepository.getNetworkSetting(settingName) ?: return
            Timber.d("NetworkApplicationSettings-updateNetworkSetting: $settingName = $value")
            networkInteractor.updateUserSetting(settingName, value, timestamp)
        }
    }

    fun updateProfileLanguage() {
        val timestamp = markLocalSettingUpdated(NetworkSettingNames.PROFILE_LANGUAGE_CODE)
        if (networkInteractor.signedIn) {
            val language = localeInteractor.getCurrentLocaleLanguage()
            Timber.d("NetworkApplicationSettings-updateProfileLanguage: $language")
            networkInteractor.updateUserSetting(
                NetworkSettingNames.PROFILE_LANGUAGE_CODE,
                language,
                timestamp
            )
        }
    }

    fun updateSensorsOrder() {
        val timestamp = markLocalSettingUpdated(NetworkSettingNames.SENSOR_ORDER)
        if (networkInteractor.signedIn) {
            val value = preferencesRepository.getNetworkSetting(NetworkSettingNames.SENSOR_ORDER)
            if (value != null) {
                Timber.d("NetworkApplicationSettings-updateSensorsOrder: $value")
                networkInteractor.updateUserSetting(NetworkSettingNames.SENSOR_ORDER, value, timestamp)
            }
        }
    }
}
