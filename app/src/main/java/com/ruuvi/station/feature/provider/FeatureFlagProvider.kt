package com.ruuvi.station.feature.provider

import com.ruuvi.station.feature.data.Feature

interface FeatureFlagProvider {
    val priority: Int
    fun isFeatureEnabled(feature: Feature): Boolean
    fun hasFeature(feature: Feature): Boolean
}

const val MAX_PRIORITY = 1
const val MEDIUM_PRIORITY = 2
const val MIN_PRIORITY = 3