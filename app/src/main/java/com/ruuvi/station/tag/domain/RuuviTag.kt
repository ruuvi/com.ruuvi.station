package com.ruuvi.station.tag.domain

import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.units.model.EnvironmentValue
import java.util.Date

data class RuuviTag(
    val id: String,
    val name: String,
    val displayName: String,
    var temperatureOffset: Double?,
    var humidityOffset: Double?,
    var pressureOffset: Double?,
    val defaultBackground: Int,
    val userBackground: String?,
    val networkBackground: String?,
    val alarmSensorStatus: AlarmSensorStatus = AlarmSensorStatus.NoAlarms,
    val lastSync: Date?,
    val networkLastSync: Date?,
    val networkSensor: Boolean,
    val owner: String?,
    val subscriptionName: String?,
    var firmware: String?,
    val latestMeasurement: SensorMeasurements?
)

data class SensorMeasurements(
    val temperatureValue: EnvironmentValue,
    val humidityValue: EnvironmentValue?,
    val pressureValue: EnvironmentValue?,
    val movementValue: EnvironmentValue?,
    val voltageValue: EnvironmentValue,
    val rssiValue: EnvironmentValue,
    val accelerationX: Double?,
    val accelerationY: Double?,
    val accelerationZ: Double?,
    val measurementSequenceNumber: Int,
    val txPower: Double,
    val connectable: Boolean?,
    val dataFormat: Int,
    val updatedAt: Date?,
)

fun SensorMeasurements.isLowBattery(): Boolean {
    val temperature = temperatureValue.value
    val voltage = voltageValue.value
    return when {
        temperature <= -20 && voltage < 2 && voltage > 0 -> true
        temperature > -20 && temperature < 0 && voltage < 2.3 && voltage > 0 -> true
        temperature >= 0 && voltage < 2.5 && voltage > 0 -> true
        else -> false
    }
}

fun RuuviTag.isLowBattery(): Boolean {
    return this.latestMeasurement?.isLowBattery() ?: false
}

fun RuuviTag.canUseCloudAlerts(): Boolean {
    return !this.subscriptionName.isNullOrEmpty() && this.subscriptionName != "Free" && this.subscriptionName != "Basic"
}