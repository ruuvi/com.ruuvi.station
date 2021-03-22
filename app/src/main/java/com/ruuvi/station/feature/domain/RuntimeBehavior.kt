package com.ruuvi.station.feature.domain

import com.ruuvi.station.feature.data.Feature
import com.ruuvi.station.feature.provider.FeatureFlagProvider
import com.ruuvi.station.feature.provider.RemoteFeatureFlagProvider
import java.util.concurrent.CopyOnWriteArrayList

class RuntimeBehavior{
    private val providers = CopyOnWriteArrayList<FeatureFlagProvider>()

    fun isFeatureEnabled(feature: Feature): Boolean {
        return providers.filter { it.hasFeature(feature) }
            .sortedBy(FeatureFlagProvider::priority)
            .firstOrNull()
            ?.isFeatureEnabled(feature)
            ?: feature.defaultValue
    }

    fun addProvider(provider: FeatureFlagProvider) = providers.add(provider)

    fun refreshFeatureFlags() {
        providers.filter { it is RemoteFeatureFlagProvider }.forEach { (it as RemoteFeatureFlagProvider).refreshFeatureFlags() }
    }
}