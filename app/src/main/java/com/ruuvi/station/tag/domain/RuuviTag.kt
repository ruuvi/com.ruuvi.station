package com.ruuvi.station.tag.domain

import com.ruuvi.station.alarm.domain.AlarmStatus
import java.util.Date

data class RuuviTag(
    val id: String,
    val name: String,
    val displayName: String,
    val rssi: Int,
    val temperature: Double,
    val humidity: Double?,
    val pressure: Double?,
    val movementCounter: Int?,
    val temperatureString: String,
    val humidityString: String,
    val pressureString: String,
    val movementCounterString: String,
    val defaultBackground: Int,
    val dataFormat: Int,
    val updatedAt: Date?,
    val userBackground: String?,
    val status: AlarmStatus = AlarmStatus.NO_ALARM,
    val connectable: Boolean?,
    val lastSync: Date?,
    val networkLastSync: Date?,
    val networkSensor: Boolean,
    val owner: String?
)