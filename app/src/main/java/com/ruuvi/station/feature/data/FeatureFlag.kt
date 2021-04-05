package com.ruuvi.station.feature.data

enum class FeatureFlag (
    override val key: String,
    override val title: String,
    override val description: String,
    override val defaultValue: Boolean
) : Feature {
    RUUVI_NETWORK("android_network", "Ruuvi Network", "Enable access to Ruuvi Network (beta)", false)
}