package com.ruuvi.station.feature.provider

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.ruuvi.station.R
import com.ruuvi.station.feature.data.Feature
import com.ruuvi.station.feature.data.FeatureFlag
import timber.log.Timber

class FirebaseFeatureFlagProvider() : FeatureFlagProvider, RemoteFeatureFlagProvider {
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
    }

    override val priority: Int = MEDIUM_PRIORITY

    override fun isFeatureEnabled(feature: Feature): Boolean {
        Timber.d("remoteConfig = ${remoteConfig.getString(feature.key)}")
        return remoteConfig.getBoolean(feature.key)
    }

    override fun hasFeature(feature: Feature): Boolean {
        Timber.d("remoteConfig.hasFeature")
        return when (feature) {
            FeatureFlag.NEW_SENSOR_CARD -> true
            FeatureFlag.VISIBLE_MEASUREMENTS -> true
            else -> false
        }
    }

    override fun refreshFeatureFlags() {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Timber.d("fetch remoteConfig fetch ${task.result}")
            } else {
                Timber.d("fetch remoteConfig failed ${task.exception}")
            }
        }
    }
}