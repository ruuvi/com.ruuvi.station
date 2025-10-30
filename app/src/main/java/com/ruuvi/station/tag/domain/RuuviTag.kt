package com.ruuvi.station.tag.domain

import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.tag.domain.RuuviTag.Companion.dataFormatIsAir
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
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
    var defaultDisplayOrder: Boolean,
    var displayOrder: List<UnitType>,
    var possibleDisplayOptions: List<UnitType>,
    val valuesToDisplay: List<EnvironmentValue>,
    val latestMeasurement: SensorMeasurements?
) {
    fun getDefaultName(): String = MacAddressUtils.getDefaultName(id, isAir())

    fun getSource(): UpdateSource {

        return if (latestMeasurement?.updatedAt == networkLastSync) {
            UpdateSource.Cloud
        } else {
            UpdateSource.Advertisement
        }
    }

    companion object {
        fun dataFormatIsAir(dataFormat: Int?): Boolean {
            return dataFormat == 0xE0 || dataFormat == 0xF0 || dataFormat == 0xE1 || dataFormat == 0x06
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
    var pm10: EnvironmentValue?,
    var pm25: EnvironmentValue?,
    var pm40: EnvironmentValue?,
    var pm100: EnvironmentValue?,
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

fun RuuviTag.isAir(): Boolean = dataFormatIsAir(latestMeasurement?.dataFormat)

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

val sensorMeasurementsPreview =
    SensorMeasurements(
        aqi = null,
        temperature = EnvironmentValue(
            original = 22.75,
            value = 22.75,
            accuracy = Accuracy.Accuracy1,
            valueWithUnit = "22.7 C",
            valueWithoutUnit = "22.7",
            unitString = "C",
            unitType = UnitType.TemperatureUnit.Celsius
        ),
        humidity = null,
        pressure = null,
        movement = null,
        voltage = EnvironmentValue(
            original = 2.89,
            value = 2.89,
            accuracy = Accuracy.Accuracy2,
            valueWithUnit = "2.89 V",
            valueWithoutUnit = "2.29",
            unitString = "V",
            unitType = UnitType.BatteryVoltageUnit.Volt
        ),
        rssi = EnvironmentValue(
            original = -44.0,
            value = -44.0,
            accuracy = Accuracy.Accuracy0,
            valueWithUnit = "-44 dBm",
            valueWithoutUnit = "-44",
            unitString = "dBm",
            unitType = UnitType.SignalStrengthUnit.SignalDbm
        ),
        accelerationX = null,
        accelerationY = null,
        accelerationZ = null,
        measurementSequenceNumber = 2323,
        txPower = -4.0,
        pm10 = null,
        pm25 = null,
        pm40 = null,
        pm100 = null,
        co2 = null,
        voc = null,
        nox = null,
        luminosity = null,
        dBaAvg = null,
        dBaPeak = null,
        connectable = null,
        dataFormat = 5,
        updatedAt = Date()
    )

val ruuviTagPreview = RuuviTag(
    id = "AA:BB:CC:DD:EE:FF",
    name = "Ruuvi Tag Preview",
    displayName = "Ruuvi Tag Preview",
    temperatureOffset = 0.0,
    humidityOffset = 0.0,
    pressureOffset = 0.0,
    defaultBackground = 0,
    userBackground = "",
    networkBackground = "",
    alarmSensorStatus = AlarmSensorStatus.NotTriggered,
    lastSync = Date(),
    networkLastSync = Date(),
    networkSensor = true,
    owner = "",
    subscriptionName = "",
    firmware = "",
    defaultDisplayOrder = true,
    displayOrder = listOf(),
    valuesToDisplay = listOf(),
    possibleDisplayOptions = listOf(),
    latestMeasurement = sensorMeasurementsPreview
)