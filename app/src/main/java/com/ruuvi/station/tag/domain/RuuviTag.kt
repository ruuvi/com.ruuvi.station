package com.ruuvi.station.tag.domain

import java.util.Date

data class RuuviTag(
    val id: String,
    val name: String,
    val displayName: String,
    val rssi: Int,
    val temperature: Double,
    val humidity: Double,
    val pressure: Double,
    val updatedAt: Date?,
    val temperatureString: String,
    val humidityString: String
)