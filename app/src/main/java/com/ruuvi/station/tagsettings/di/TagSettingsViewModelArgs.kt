package com.ruuvi.station.tagsettings.di

data class TagSettingsViewModelArgs(
    val tagId: String,
    val newSensor: Boolean = false
)