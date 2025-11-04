package com.ruuvi.station.feature.data

enum class FeatureFlag (
    override val key: String,
    override val title: String,
    override val description: String,
    override val defaultValue: Boolean
) : Feature {
    VISIBLE_MEASUREMENTS("android_visible_measurements", "Visible Measurements", "Enable visible measurements", true),
    NEW_SENSOR_CARD("android_new_sensor_card", "New sensor card UI", "Enable new sensor card UI", true)
}