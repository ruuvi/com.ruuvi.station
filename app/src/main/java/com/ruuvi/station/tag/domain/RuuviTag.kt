package com.ruuvi.station.tag.domain

import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.util.MacAddressUtils
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
) {
    fun getDefaultName(): String = MacAddressUtils.getDefaultName(id)

    fun getSource(): UpdateSource {
        return if (latestMeasurement?.updatedAt == networkLastSync) {
            UpdateSource.Cloud
        } else {
            UpdateSource.Advertisement
        }
    }
}

data class SensorMeasurements(
    val aqi: EnvironmentValue?,
    val temperature: EnvironmentValue?,
    val humidity: EnvironmentValue?,
    val pressure: EnvironmentValue?,
    val movement: EnvironmentValue?,
    val voltage: EnvironmentValue,
    val rssi: EnvironmentValue,
    val accelerationX: Double?,
    val accelerationY: Double?,
    val accelerationZ: Double?,
    val measurementSequenceNumber: Int,
    val txPower: Double,
    var pm1: EnvironmentValue?,
    var pm25: EnvironmentValue?,
    var pm4: EnvironmentValue?,
    var pm10: EnvironmentValue?,
    var co2: EnvironmentValue?,
    var voc: EnvironmentValue?,
    var nox: EnvironmentValue?,
    var luminosity: EnvironmentValue?,
    var dBaAvg: EnvironmentValue?,
    var dBaPeak: EnvironmentValue?,
    val connectable: Boolean?,
    val dataFormat: Int,
    val updatedAt: Date
) {
    val aqiScore: AQI = AQI.getAQI(this)
}

fun SensorMeasurements.isLowBattery(): Boolean {
    val voltage = voltage.value
    val temperature = temperature?.value ?: return voltage < 2.5
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

fun RuuviTag.isAir(): Boolean =
    this.latestMeasurement?.dataFormat == 0xE0 || this.latestMeasurement?.dataFormat == 0xF0

sealed class UpdateSource() {
    abstract fun getDescriptionResource(): Int
    abstract fun getIconResource(): Int

    data object Cloud: UpdateSource() {
        override fun getDescriptionResource(): Int {
            return R.string.cloud
        }

        override fun getIconResource(): Int {
            return R.drawable.ic_icon_gateway
        }
    }

    data object Advertisement: UpdateSource() {
        override fun getDescriptionResource(): Int {
            return R.string.advertisement
        }

        override fun getIconResource(): Int {
            return R.drawable.ic_icon_bluetooth
        }
    }
}