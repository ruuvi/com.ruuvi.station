package com.ruuvi.station.feature.provider

import android.content.Context
import android.content.SharedPreferences
import com.ruuvi.station.feature.data.Feature
import androidx.core.content.edit
import com.ruuvi.station.feature.data.FeatureFlag

class RuntimeFeatureFlagProvider(applicationContext: Context) : FeatureFlagProvider {

    private val preferences: SharedPreferences =
        applicationContext.getSharedPreferences("runtime.featureflags", Context.MODE_PRIVATE)

    override val priority = MAX_PRIORITY

    override fun isFeatureEnabled(feature: Feature): Boolean =
        preferences.getBoolean(feature.key, feature.defaultValue)

    override fun hasFeature(feature: Feature): Boolean {
        return if (feature == FeatureFlag.NEW_SENSOR_CARD) {
            true
        } else {
            preferences.contains(feature.key)
        }
    }

    fun setFeatureEnabled(feature: Feature, enabled: Boolean) =
        preferences.edit (commit = true) { putBoolean(feature.key, enabled) }
}