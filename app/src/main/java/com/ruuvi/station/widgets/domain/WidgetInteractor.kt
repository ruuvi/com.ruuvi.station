package com.ruuvi.station.widgets.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.data.response.SensorDenseResponse
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.domain.AccelerationAxis
import com.ruuvi.station.units.domain.AccelerationConverter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.extensions.*
import com.ruuvi.station.widgets.data.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.*
import kotlin.Exception

class WidgetInteractor (
    val context: Context,
    val tagRepository: TagRepository,
    val cloudInteractor: RuuviNetworkInteractor,
    val unitsConverter: UnitsConverter,
    val accelerationConverter: AccelerationConverter
) {
    fun getCloudSensorsList() = tagRepository.getFavoriteSensors()

    suspend fun getComplexWidgetData(sensorId: String, settings: ComplexWidgetPreferenceItem?): ComplexWidgetData {
        val sensorFav = tagRepository.getFavoriteSensorById(sensorId)
            ?: return emptyComplexResult(sensorId)

        if (isCloudSensor(sensorFav)) {
            val cloudData = getComplexDataFromCloud(sensorFav, settings)
            val localData = getComplexLocalData(sensorFav, settings)

            return if (cloudData.timestamp > localData.timestamp) {
                cloudData
            } else {
                localData
            }
        } else {
            return getComplexLocalData(sensorFav, settings)
        }
    }

    private suspend fun getComplexDataFromCloud(sensorFav: RuuviTag, settings: ComplexWidgetPreferenceItem?): ComplexWidgetData
    {
        val sensorId = sensorFav.id
        try {
            val lastMeasurement = getSensorLatestValues(sensorId)
            val result = ComplexWidgetData(
                sensorId = sensorId,
                timestamp = lastMeasurement?.updatedAt ?: Date(0),
                displayName = sensorFav.displayName,
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
                        lastMeasurement.voltage,
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
            Timber.e(e)
            return emptyComplexResult(sensorId)
        }
    }


    private fun getComplexLocalData(sensorFav: RuuviTag, settings: ComplexWidgetPreferenceItem?): ComplexWidgetData {
        val sensorId = sensorFav.id
        val lastMeasurement = sensorFav.latestMeasurement

        val result = ComplexWidgetData(
            sensorId = sensorId,
            timestamp = lastMeasurement?.updatedAt ?: Date(0),
            displayName = sensorFav.displayName,
            sensorValues = listOf(),
            updated = null
        )

        if (lastMeasurement != null) {
            val temperatureValue = SensorValue(
                type = WidgetType.TEMPERATURE,
                sensorValue = lastMeasurement.temperatureValue.valueWithoutUnit,
                unit = lastMeasurement.temperatureValue.unitString
            )

            val humidityValue = SensorValue(
                type = WidgetType.HUMIDITY,
                sensorValue = lastMeasurement.humidityValue?.valueWithoutUnit ?: UnitsConverter.NO_VALUE_AVAILABLE,
                unit = lastMeasurement.humidityValue?.unitString ?: context.getString(unitsConverter.getHumidityUnit().unit)
            )

            val pressureValue = SensorValue(
                type = WidgetType.PRESSURE,
                sensorValue = lastMeasurement.pressureValue?.valueWithoutUnit ?: UnitsConverter.NO_VALUE_AVAILABLE,
                unit = lastMeasurement.pressureValue?.unitString ?: context.getString(unitsConverter.getPressureUnit().unit)
            )

            val movementsValue = SensorValue(
                type = WidgetType.MOVEMENT,
                sensorValue = lastMeasurement.movementValue?.valueWithoutUnit ?: UnitsConverter.NO_VALUE_AVAILABLE,
                unit = lastMeasurement.movementValue?.unitString ?: context.getString(R.string.movements)
            )

            val voltageValue = SensorValue(
                type = WidgetType.VOLTAGE,
                sensorValue = lastMeasurement.voltageValue.valueWithoutUnit,
                unit = lastMeasurement.voltageValue.unitString
            )

            val signalStrengthValue = SensorValue(
                type = WidgetType.SIGNAL_STRENGTH,
                sensorValue = lastMeasurement.rssiValue.valueWithoutUnit ,
                unit = lastMeasurement.rssiValue.unitString
            )

            val accelerationXValue = SensorValue(
                type = WidgetType.ACCELERATION_X,
                sensorValue = accelerationConverter.getAccelerationStringWithoutUnit(lastMeasurement.accelerationX),
                unit = accelerationConverter.getAccelerationUnit(AccelerationAxis.AXIS_X)
            )

            val accelerationYValue = SensorValue(
                type = WidgetType.ACCELERATION_Y,
                sensorValue = accelerationConverter.getAccelerationStringWithoutUnit(lastMeasurement.accelerationY),
                unit = accelerationConverter.getAccelerationUnit(AccelerationAxis.AXIS_Y)
            )

            val accelerationZValue = SensorValue(
                type = WidgetType.ACCELERATION_Z,
                sensorValue = accelerationConverter.getAccelerationStringWithoutUnit(lastMeasurement.accelerationZ),
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
    }


    private var sensorDenseResponse: SensorDenseResponse? = null
    private var lastSyncDate: Date? = null
    private val mutex = Mutex()

    private suspend fun getSensorDataFromCloud(): SensorDenseResponse? = mutex.withLock {
        if (sensorDenseResponse == null ||
            lastSyncDate == null ||
            lastSyncDate?.diffGreaterThan(60 * 1000L) == true
        ) {
            try {
                sensorDenseResponse = cloudInteractor.getSensorDenseLastData()
                lastSyncDate = Date()
            } catch (e: Exception) {
                return sensorDenseResponse ?: throw e
            }
        }
        sensorDenseResponse
    }

    suspend fun getSensorLatestValues(sensorId: String): DecodedSensorData? {
        val sensorFav = tagRepository.getFavoriteSensorById(sensorId)
        if (sensorFav == null || !isCloudSensor(sensorFav)) {
            return null
        }

        val lastDataResponse = getSensorDataFromCloud()
        val sensorInfo = lastDataResponse?.data?.sensors?.first{it.sensor == sensorId}
        val lastMeasurement = sensorInfo?.measurements?.maxByOrNull { it.timestamp }
        if (lastDataResponse?.isSuccess() == true && lastMeasurement != null) {
            val decoded =  BluetoothLibrary.decode(sensorId, lastMeasurement.data, lastMeasurement.rssi)
            decoded.temperature?.let { temperature ->
                decoded.temperature = temperature + (sensorInfo.offsetTemperature ?: 0.0)
            }
            decoded.humidity?.let { humidity ->
                decoded.humidity = humidity + (sensorInfo.offsetHumidity ?: 0.0)
            }
            decoded.pressure?.let { pressure ->
                decoded.pressure = pressure + (sensorInfo.offsetPressure ?: 0.0)
            }
            val updatedDate = Date(lastMeasurement.timestamp * 1000)

            return DecodedSensorData(decoded, updatedDate)
        } else {
            return null
        }
    }

    suspend fun getSimpleWidgetData(sensorId: String, widgetType: WidgetType): SimpleWidgetData? {
        val sensorFav = tagRepository.getFavoriteSensorById(sensorId) ?: return emptySimpleResult(sensorId)

        if (isCloudSensor(sensorFav)) {
            val cloudData = getSimpleDataFromCloud(sensorFav, widgetType)
            val localData = getSimpleLocalData(sensorFav, widgetType)
            if (cloudData?.timestamp ?: Date(0L) > localData.timestamp) {
                return cloudData
            } else {
                return localData
            }
        } else {
            return getSimpleLocalData(sensorFav, widgetType)
        }
    }

    fun getSimpleLocalData(tag: RuuviTag, widgetType: WidgetType): SimpleWidgetData {
        val sensorId = tag.id
        if (tag.latestMeasurement != null) {
            var unit = ""
            var sensorValue = ""
            when (widgetType) {
                WidgetType.TEMPERATURE -> {
                    unit = context.getString(unitsConverter.getTemperatureUnit().unit)
                    sensorValue = tag.latestMeasurement.temperatureValue.valueWithoutUnit
                }
                WidgetType.HUMIDITY -> {
                    unit = context.getString(unitsConverter.getHumidityUnit().unit)
                    sensorValue = tag.latestMeasurement.humidityValue?.valueWithoutUnit ?: ""
                }
                WidgetType.PRESSURE -> {
                    unit = context.getString(unitsConverter.getPressureUnit().unit)
                    sensorValue = tag.latestMeasurement.pressureValue?.valueWithoutUnit ?: ""
                }
                WidgetType.MOVEMENT -> {
                    unit = context.getString(R.string.movements)
                    sensorValue = tag.latestMeasurement.movementValue?.valueWithoutUnit ?: ""
                }
                WidgetType.VOLTAGE -> {
                    unit = context.getString(R.string.voltage_unit)
                    sensorValue = tag.latestMeasurement.voltageValue.valueWithoutUnit
                }
                WidgetType.SIGNAL_STRENGTH -> {
                    unit = context.getString(R.string.signal_unit)
                    sensorValue = tag.latestMeasurement.rssiValue.valueWithoutUnit
                }
                WidgetType.ACCELERATION_X -> {
                    unit = accelerationConverter.getAccelerationUnit(AccelerationAxis.AXIS_X)
                    sensorValue = accelerationConverter.getAccelerationStringWithoutUnit(tag.latestMeasurement.accelerationX)
                }
                WidgetType.ACCELERATION_Y -> {
                    unit = accelerationConverter.getAccelerationUnit(AccelerationAxis.AXIS_Y)
                    sensorValue = accelerationConverter.getAccelerationStringWithoutUnit(tag.latestMeasurement.accelerationY)
                }
                WidgetType.ACCELERATION_Z -> {
                    unit = accelerationConverter.getAccelerationUnit(AccelerationAxis.AXIS_Z)
                    sensorValue = accelerationConverter.getAccelerationStringWithoutUnit(tag.latestMeasurement.accelerationZ)
                }
            }
            val updatedDate = tag.latestMeasurement.updatedAt
            val updated = if (updatedDate.diffGreaterThan(hours24)) {
                updatedDate.localizedDate(context)
            } else {
                updatedDate.localizedTime(context)
            }
            return SimpleWidgetData(
                sensorId = sensorId,
                timestamp = updatedDate,
                displayName = tag.displayName,
                sensorValue = sensorValue,
                unit = unit,
                updated = updated
            )
        } else {
            return emptySimpleResult(sensorId)
        }
    }

    suspend fun getSimpleDataFromCloud(tag: RuuviTag, widgetType: WidgetType): SimpleWidgetData? {
        val sensorId = tag.id
        try {
            val sensorsData = getSensorDataFromCloud()
            val lastData = sensorsData?.data?.sensors?.firstOrNull{it.sensor == sensorId}

            Timber.d(lastData.toString())

            val measurement = lastData?.measurements?.maxByOrNull { it.timestamp }
            if (measurement != null) {
                val decoded = BluetoothLibrary.decode(sensorId, measurement.data, measurement.rssi)
                decoded.temperature?.let { temperature ->
                    decoded.temperature = temperature + (lastData.offsetTemperature ?: 0.0)
                }
                decoded.humidity?.let { humidity ->
                    decoded.humidity = humidity + (lastData.offsetHumidity ?: 0.0)
                }
                decoded.pressure?.let { pressure ->
                    decoded.pressure = pressure + (lastData.offsetPressure ?: 0.0)
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
                    timestamp = updatedDate,
                    displayName = tag.displayName,
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

    private fun emptySimpleResult(sensorId: String): SimpleWidgetData = SimpleWidgetData(sensorId, Date(0), context.getString(R.string.no_data), "", "", null)

    fun emptyComplexResult(sensorId: String): ComplexWidgetData = ComplexWidgetData(sensorId, Date(0), context.getString(R.string.no_data), emptyList(), null)

    private fun isCloudSensor(sensor: RuuviTag) = sensor.networkLastSync != null
}