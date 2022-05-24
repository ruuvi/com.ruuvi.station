package com.ruuvi.station.widgets.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.domain.AccelerationAxis
import com.ruuvi.station.units.domain.AccelerationConverter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.extensions.*
import com.ruuvi.station.widgets.data.*
import timber.log.Timber
import java.lang.Exception
import java.util.*

class WidgetInteractor (
    val context: Context,
    val tagRepository: TagRepository,
    val cloudInteractor: RuuviNetworkInteractor,
    val unitsConverter: UnitsConverter,
    val accelerationConverter: AccelerationConverter
) {
    fun getCloudSensorsList() = tagRepository.getFavoriteSensors().filter { it.networkLastSync != null }

    suspend fun getComplexWidgetData(sensorId: String, settings: ComplexWidgetPreferenceItem?): ComplexWidgetData {
        val sensorFav = tagRepository.getFavoriteSensorById(sensorId)

        if (sensorFav == null || !canReturnData(sensorFav)) {
            return emptyComplexResult(sensorId)
        }

        try {
            val lastMeasurement = getSensorLatestValues(sensorId)
            val result = ComplexWidgetData(
                sensorId = sensorId,
                displayName = sensorFav?.displayName ?: sensorId,
                sensorValues = listOf(),
                updated = null
            )

            if (lastMeasurement != null) {
                val temperatureValue = SensorValue(
                    type = WidgetType.TEMPERATURE,
                    sensorValue = unitsConverter.getTemperatureStringWithoutUnit(lastMeasurement.temperature),
                    unit = context.getString(unitsConverter.getTemperatureUnit().unit)
                )

                val humidityValue = SensorValue(
                    type = WidgetType.HUMIDITY,
                    sensorValue = unitsConverter.getHumidityStringWithoutUnit(
                        lastMeasurement.humidity,
                        lastMeasurement.temperature ?: 0.0
                    ),
                    unit = context.getString(unitsConverter.getHumidityUnit().unit)
                )

                val pressureValue = SensorValue(
                    type = WidgetType.PRESSURE,
                    sensorValue = unitsConverter.getPressureStringWithoutUnit(lastMeasurement.pressure),
                    unit = context.getString(unitsConverter.getPressureUnit().unit)
                )

                val movementsValue = SensorValue(
                    type = WidgetType.MOVEMENT,
                    sensorValue = lastMeasurement.movementCounter.toString(),
                    unit = context.getString(R.string.movements)
                )

                val voltageValue = SensorValue(
                    type = WidgetType.VOLTAGE,
                    sensorValue = context.getString(
                        R.string.voltage_reading,
                        lastMeasurement.voltage.toString(),
                        ""
                    ).trim(),
                    unit = context.getString(R.string.voltage_unit)
                )

                val signalStrengthValue = SensorValue(
                    type = WidgetType.SIGNAL_STRENGTH,
                    sensorValue = lastMeasurement.rssi.toString(),
                    unit = context.getString(R.string.signal_unit)
                )

                val accelerationXValue = SensorValue(
                    type = WidgetType.ACCELERATION_X,
                    sensorValue = accelerationConverter.getAccelerationStringWithoutUnit(lastMeasurement.accelX),
                    unit = accelerationConverter.getAccelerationUnit(AccelerationAxis.AXIS_X)
                )

                val accelerationYValue = SensorValue(
                    type = WidgetType.ACCELERATION_Y,
                    sensorValue = accelerationConverter.getAccelerationStringWithoutUnit(lastMeasurement.accelY),
                    unit = accelerationConverter.getAccelerationUnit(AccelerationAxis.AXIS_Y)
                )

                val accelerationZValue = SensorValue(
                    type = WidgetType.ACCELERATION_Z,
                    sensorValue = accelerationConverter.getAccelerationStringWithoutUnit(lastMeasurement.accelZ),
                    unit = accelerationConverter.getAccelerationUnit(AccelerationAxis.AXIS_Z)
                )

                result.updated = if (lastMeasurement.updatedAt.diffGreaterThan(hours24)) {
                    lastMeasurement.updatedAt.localizedDate(context)
                } else {
                    lastMeasurement.updatedAt.localizedTime(context)
                }

                val sensorValues: MutableList<SensorValue> = mutableListOf()
                if (settings?.checkedTemperature == true) sensorValues.add(temperatureValue)
                if (settings?.checkedHumidity == true) sensorValues.add(humidityValue)
                if (settings?.checkedPressure == true) sensorValues.add(pressureValue)
                if (settings?.checkedMovement == true) sensorValues.add(movementsValue)
                if (settings?.checkedVoltage == true) sensorValues.add(voltageValue)
                if (settings?.checkedSignalStrength == true) sensorValues.add(signalStrengthValue)
                if (settings?.checkedAccelerationX == true) sensorValues.add(accelerationXValue)
                if (settings?.checkedAccelerationY == true) sensorValues.add(accelerationYValue)
                if (settings?.checkedAccelerationZ == true) sensorValues.add(accelerationZValue)
                result.sensorValues = sensorValues
            }

            return result
        } catch (e: Exception) {
            return emptyComplexResult(sensorId)
        }
    }

    suspend fun getSensorLatestValues(sensorId: String): DecodedSensorData? {
        val sensorFav = tagRepository.getFavoriteSensorById(sensorId)
        if (sensorFav == null || !canReturnData(sensorFav)) {
            return null
        }

        val lastDataResponse = cloudInteractor.getSensorLastData(sensorId)
        val lastMeasurement = lastDataResponse?.data?.measurements?.maxByOrNull { it.timestamp }
        if (lastDataResponse?.isSuccess() == true && lastMeasurement != null) {
            val decoded =  BluetoothLibrary.decode(sensorId, lastMeasurement.data, lastMeasurement.rssi)
            decoded.temperature?.let { temperature ->
                decoded.temperature = temperature + (lastDataResponse.data.offsetTemperature ?: 0.0)
            }
            decoded.humidity?.let { humidity ->
                decoded.humidity = humidity + (lastDataResponse.data.offsetHumidity ?: 0.0)
            }
            decoded.pressure?.let { pressure ->
                decoded.pressure = pressure + (lastDataResponse.data.offsetPressure ?: 0.0)
            }
            val updatedDate = Date(lastMeasurement.timestamp * 1000)

            return DecodedSensorData(decoded, updatedDate)
        } else {
            return null
        }
    }

    suspend fun getSimpleWidgetData(sensorId: String, widgetType: WidgetType): SimpleWidgetData? {
        var sensorFav = tagRepository.getFavoriteSensorById(sensorId)
        if (sensorFav == null || !canReturnData(sensorFav)) {
            return emptySimpleResult(sensorId)
        }

        try {
            val lastData = cloudInteractor.getSensorLastData(sensorId)

            Timber.d(lastData.toString())

            val measurement = lastData?.data?.measurements?.maxByOrNull { it.timestamp }
            if (lastData?.isSuccess() == true && measurement != null) {
                val decoded = BluetoothLibrary.decode(sensorId, measurement.data, measurement.rssi)
                decoded.temperature?.let { temperature ->
                    decoded.temperature = temperature + (lastData.data.offsetTemperature ?: 0.0)
                }
                decoded.humidity?.let { humidity ->
                    decoded.humidity = humidity + (lastData.data.offsetHumidity ?: 0.0)
                }
                decoded.pressure?.let { pressure ->
                    decoded.pressure = pressure + (lastData.data.offsetPressure ?: 0.0)
                }

                val updatedDate = Date(measurement.timestamp * 1000)

                var unit = ""
                var sensorValue = ""
                when (widgetType) {
                    WidgetType.TEMPERATURE -> {
                        unit = context.getString(unitsConverter.getTemperatureUnit().unit)
                        sensorValue =
                            unitsConverter.getTemperatureStringWithoutUnit(decoded.temperature)
                    }
                    WidgetType.HUMIDITY -> {
                        unit = context.getString(unitsConverter.getHumidityUnit().unit)
                        sensorValue = unitsConverter.getHumidityStringWithoutUnit(
                            decoded.humidity,
                            decoded.temperature ?: 0.0
                        )
                    }
                    WidgetType.PRESSURE -> {
                        unit = context.getString(unitsConverter.getPressureUnit().unit)
                        sensorValue = unitsConverter.getPressureStringWithoutUnit(decoded.pressure)
                    }
                    WidgetType.MOVEMENT -> {
                        unit = context.getString(R.string.movements)
                        sensorValue = decoded.movementCounter.toString()
                    }
                    WidgetType.VOLTAGE -> {
                        unit = context.getString(R.string.voltage_unit)
                        sensorValue =
                            context.getString(R.string.voltage_reading, decoded.voltage.toString(), "")
                                .trim()
                    }
                    WidgetType.SIGNAL_STRENGTH -> {
                        unit = context.getString(R.string.signal_unit)
                        sensorValue = decoded.rssi.toString()
                    }
                    WidgetType.ACCELERATION_X -> {
                        unit = accelerationConverter.getAccelerationUnit(AccelerationAxis.AXIS_X)
                        sensorValue = accelerationConverter.getAccelerationStringWithoutUnit(decoded.accelX)
                    }
                    WidgetType.ACCELERATION_Y -> {
                        unit = accelerationConverter.getAccelerationUnit(AccelerationAxis.AXIS_Y)
                        sensorValue = accelerationConverter.getAccelerationStringWithoutUnit(decoded.accelY)
                    }
                    WidgetType.ACCELERATION_Z -> {
                        unit = accelerationConverter.getAccelerationUnit(AccelerationAxis.AXIS_Z)
                        sensorValue = accelerationConverter.getAccelerationStringWithoutUnit(decoded.accelZ)
                    }
                }

                val updated = if (updatedDate.diffGreaterThan(hours24)) {
                    updatedDate.localizedDate(context)
                } else {
                    updatedDate.localizedTime(context)
                }

                return SimpleWidgetData(
                    sensorId = sensorId,
                    displayName = sensorFav.displayName,
                    sensorValue = sensorValue,
                    unit = unit,
                    updated = updated
                )
            } else {
                return emptySimpleResult(sensorId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Widget update exception")
            return null
        }
    }

    private fun emptyResult(sensorId: String): WidgetData = WidgetData(sensorId)

    private fun emptySimpleResult(sensorId: String): SimpleWidgetData = SimpleWidgetData(sensorId, context.getString(R.string.no_data), "", "", null)

    fun emptyComplexResult(sensorId: String): ComplexWidgetData = ComplexWidgetData(sensorId, context.getString(R.string.no_data), emptyList(), null)

    private fun canReturnData(sensor: RuuviTag) = sensor.networkLastSync != null
}