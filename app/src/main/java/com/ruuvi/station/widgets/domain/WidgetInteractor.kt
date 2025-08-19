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
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.UnitType
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

                val aqiValue = SensorValue(
                    type = WidgetType.AIR_QUALITY,
                    sensorValue = if (lastMeasurement.co2 != null && lastMeasurement.pm25 != null) AQI.getAQI(pm25 = lastMeasurement.pm25, co2 = lastMeasurement.co2).scoreString else UNDEFINED_VALUE,
                    unit = context.getString(UnitType.AirQuality.AqiIndex.unit)
                )

                val luminosityValue = SensorValue(
                    type = WidgetType.LUMINOSITY,
                    sensorValue = lastMeasurement.luminosity?.let { context.getString(UnitType.Luminosity.Lux.defaultAccuracy.nameTemplateId, it, "") } ?: UNDEFINED_VALUE,
                    unit = context.getString(UnitType.Luminosity.Lux.unit)
                )

                val pm10Value = SensorValue(
                    type = WidgetType.PM10,
                    sensorValue = lastMeasurement.pm1?.let {
                        context.getString(UnitType.PM10.Mgm3.defaultAccuracy.nameTemplateId, it, "")
                    } ?: UNDEFINED_VALUE,
                    unit = context.getString(UnitType.PM10.Mgm3.unit)
                )

                val pm25Value = SensorValue(
                    type = WidgetType.PM25,
                    sensorValue = lastMeasurement.pm25?.let {
                        context.getString(UnitType.PM25.Mgm3.defaultAccuracy.nameTemplateId, it, "")
                    } ?: UNDEFINED_VALUE,
                    unit = context.getString(UnitType.PM25.Mgm3.unit)
                )

                val pm40Value = SensorValue(
                    type = WidgetType.PM40,
                    sensorValue = lastMeasurement.pm4?.let {
                        context.getString(UnitType.PM40.Mgm3.defaultAccuracy.nameTemplateId, it, "")
                    } ?: UNDEFINED_VALUE,
                    unit = context.getString(UnitType.PM40.Mgm3.unit)
                )

                val pm100Value = SensorValue(
                    type = WidgetType.PM100,
                    sensorValue = lastMeasurement.pm10?.let {
                        context.getString(UnitType.PM100.Mgm3.defaultAccuracy.nameTemplateId, it, "")
                    } ?: UNDEFINED_VALUE,
                    unit = context.getString(UnitType.PM100.Mgm3.unit)
                )

                val co2Value = SensorValue(
                    type = WidgetType.CO2,
                    sensorValue = lastMeasurement.co2?.let {
                        context.getString(UnitType.CO2.Ppm.defaultAccuracy.nameTemplateId, it, "")
                    } ?: UNDEFINED_VALUE,
                    unit = context.getString(UnitType.CO2.Ppm.unit)
                )

                val vocValue = SensorValue(
                    type = WidgetType.VOC,
                    sensorValue = lastMeasurement.voc?.let {
                        context.getString(UnitType.VOC.VocIndex.defaultAccuracy.nameTemplateId, it, "")
                    } ?: UNDEFINED_VALUE,
                    unit = context.getString(UnitType.VOC.VocIndex.unit)
                )

                val noxValue = SensorValue(
                    type = WidgetType.NOX,
                    sensorValue = lastMeasurement.nox?.let {
                        context.getString(UnitType.NOX.NoxIndex.defaultAccuracy.nameTemplateId, it, "")
                    } ?: UNDEFINED_VALUE,
                    unit = context.getString(UnitType.NOX.NoxIndex.unit)
                )


                result.updated = if (lastMeasurement.updatedAt.diffGreaterThan(hours24)) {
                    lastMeasurement.updatedAt.localizedDate(context)
                } else {
                    lastMeasurement.updatedAt.localizedTime(context)
                }

                val sensorValues: MutableList<SensorValue> = mutableListOf()
                if (settings?.checkedAQI == true) sensorValues.add(aqiValue)
                if (settings?.checkedTemperature == true) sensorValues.add(temperatureValue)
                if (settings?.checkedHumidity == true) sensorValues.add(humidityValue)
                if (settings?.checkedPressure == true) sensorValues.add(pressureValue)
                if (settings?.checkedMovement == true) sensorValues.add(movementsValue)
                if (settings?.checkedVoltage == true) sensorValues.add(voltageValue)
                if (settings?.checkedSignalStrength == true) sensorValues.add(signalStrengthValue)
                if (settings?.checkedAccelerationX == true) sensorValues.add(accelerationXValue)
                if (settings?.checkedAccelerationY == true) sensorValues.add(accelerationYValue)
                if (settings?.checkedAccelerationZ == true) sensorValues.add(accelerationZValue)
                if (settings?.checkedLuminosity == true) sensorValues.add(luminosityValue)
                if (settings?.checkedPM10 == true) sensorValues.add(pm10Value)
                if (settings?.checkedPM25 == true) sensorValues.add(pm25Value)
                if (settings?.checkedPM40 == true) sensorValues.add(pm40Value)
                if (settings?.checkedPM100 == true) sensorValues.add(pm100Value)
                if (settings?.checkedCO2 == true) sensorValues.add(co2Value)
                if (settings?.checkedVOC == true) sensorValues.add(vocValue)
                if (settings?.checkedNOX == true) sensorValues.add(noxValue)
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
                sensorValue = lastMeasurement.temperature?.valueWithoutUnit ?: UnitsConverter.NO_VALUE_AVAILABLE,
                unit = lastMeasurement.temperature?.unitString ?: context.getString(unitsConverter.getTemperatureUnit().unit)
            )

            val humidityValue = SensorValue(
                type = WidgetType.HUMIDITY,
                sensorValue = lastMeasurement.humidity?.valueWithoutUnit ?: UnitsConverter.NO_VALUE_AVAILABLE,
                unit = lastMeasurement.humidity?.unitString ?: context.getString(unitsConverter.getHumidityUnit().unit)
            )

            val pressureValue = SensorValue(
                type = WidgetType.PRESSURE,
                sensorValue = lastMeasurement.pressure?.valueWithoutUnit ?: UnitsConverter.NO_VALUE_AVAILABLE,
                unit = lastMeasurement.pressure?.unitString ?: context.getString(unitsConverter.getPressureUnit().unit)
            )

            val movementsValue = SensorValue(
                type = WidgetType.MOVEMENT,
                sensorValue = lastMeasurement.movement?.valueWithoutUnit ?: UnitsConverter.NO_VALUE_AVAILABLE,
                unit = lastMeasurement.movement?.unitString ?: context.getString(R.string.movements)
            )

            val voltageValue = SensorValue(
                type = WidgetType.VOLTAGE,
                sensorValue = lastMeasurement.voltage.valueWithoutUnit,
                unit = lastMeasurement.voltage.unitString
            )

            val signalStrengthValue = SensorValue(
                type = WidgetType.SIGNAL_STRENGTH,
                sensorValue = lastMeasurement.rssi.valueWithoutUnit ,
                unit = lastMeasurement.rssi.unitString
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

            val aqiValue = SensorValue(
                type = WidgetType.AIR_QUALITY,
                sensorValue = if (lastMeasurement.aqi != null)  lastMeasurement.aqi.valueWithoutUnit else UNDEFINED_VALUE,
                unit = context.getString(UnitType.AirQuality.AqiIndex.unit)
            )

            val luminosityValue = SensorValue(
                type = WidgetType.LUMINOSITY,
                sensorValue = lastMeasurement.luminosity?.let { context.getString(UnitType.Luminosity.Lux.defaultAccuracy.nameTemplateId, it.value, "") } ?: UNDEFINED_VALUE,
                unit = context.getString(UnitType.Luminosity.Lux.unit)
            )

            val pm10Value = SensorValue(
                type = WidgetType.PM10,
                sensorValue = lastMeasurement.pm10?.let {
                    context.getString(UnitType.PM10.Mgm3.defaultAccuracy.nameTemplateId, it.value, "")
                } ?: UNDEFINED_VALUE,
                unit = context.getString(UnitType.PM10.Mgm3.unit)
            )

            val pm25Value = SensorValue(
                type = WidgetType.PM25,
                sensorValue = lastMeasurement.pm25?.let {
                    context.getString(UnitType.PM25.Mgm3.defaultAccuracy.nameTemplateId, it.value, "")
                } ?: UNDEFINED_VALUE,
                unit = context.getString(UnitType.PM25.Mgm3.unit)
            )

            val pm40Value = SensorValue(
                type = WidgetType.PM40,
                sensorValue = lastMeasurement.pm40?.let {
                    context.getString(UnitType.PM40.Mgm3.defaultAccuracy.nameTemplateId, it.value, "")
                } ?: UNDEFINED_VALUE,
                unit = context.getString(UnitType.PM40.Mgm3.unit)
            )

            val pm100Value = SensorValue(
                type = WidgetType.PM100,
                sensorValue = lastMeasurement.pm100?.let {
                    context.getString(UnitType.PM100.Mgm3.defaultAccuracy.nameTemplateId, it.value, "")
                } ?: UNDEFINED_VALUE,
                unit = context.getString(UnitType.PM100.Mgm3.unit)
            )

            val co2Value = SensorValue(
                type = WidgetType.CO2,
                sensorValue = lastMeasurement.co2?.let {
                    context.getString(UnitType.CO2.Ppm.defaultAccuracy.nameTemplateId, it.value, "")
                } ?: UNDEFINED_VALUE,
                unit = context.getString(UnitType.CO2.Ppm.unit)
            )

            val vocValue = SensorValue(
                type = WidgetType.VOC,
                sensorValue = lastMeasurement.voc?.let {
                    context.getString(UnitType.VOC.VocIndex.defaultAccuracy.nameTemplateId, it.value, "")
                } ?: UNDEFINED_VALUE,
                unit = context.getString(UnitType.VOC.VocIndex.unit)
            )

            val noxValue = SensorValue(
                type = WidgetType.NOX,
                sensorValue = lastMeasurement.nox?.let {
                    context.getString(UnitType.NOX.NoxIndex.defaultAccuracy.nameTemplateId, it.value, "")
                } ?: UNDEFINED_VALUE,
                unit = context.getString(UnitType.NOX.NoxIndex.unit)
            )

            result.updated = if (lastMeasurement.updatedAt.diffGreaterThan(hours24)) {
                lastMeasurement.updatedAt.localizedDate(context)
            } else {
                lastMeasurement.updatedAt.localizedTime(context)
            }

            val sensorValues: MutableList<SensorValue> = mutableListOf()
            if (settings?.checkedAQI == true) sensorValues.add(aqiValue)
            if (settings?.checkedTemperature == true) sensorValues.add(temperatureValue)
            if (settings?.checkedHumidity == true) sensorValues.add(humidityValue)
            if (settings?.checkedPressure == true) sensorValues.add(pressureValue)
            if (settings?.checkedMovement == true) sensorValues.add(movementsValue)
            if (settings?.checkedVoltage == true) sensorValues.add(voltageValue)
            if (settings?.checkedSignalStrength == true) sensorValues.add(signalStrengthValue)
            if (settings?.checkedAccelerationX == true) sensorValues.add(accelerationXValue)
            if (settings?.checkedAccelerationY == true) sensorValues.add(accelerationYValue)
            if (settings?.checkedAccelerationZ == true) sensorValues.add(accelerationZValue)
            if (settings?.checkedLuminosity == true) sensorValues.add(luminosityValue)
            if (settings?.checkedPM10 == true) sensorValues.add(pm10Value)
            if (settings?.checkedPM25 == true) sensorValues.add(pm25Value)
            if (settings?.checkedPM40 == true) sensorValues.add(pm40Value)
            if (settings?.checkedPM100 == true) sensorValues.add(pm100Value)
            if (settings?.checkedCO2 == true) sensorValues.add(co2Value)
            if (settings?.checkedVOC == true) sensorValues.add(vocValue)
            if (settings?.checkedNOX == true) sensorValues.add(noxValue)
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
        val sensorInfo = lastDataResponse?.data?.sensors?.firstOrNull{it.sensor == sensorId}
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
                    sensorValue = tag.latestMeasurement.temperature?.valueWithoutUnit ?: ""
                }
                WidgetType.HUMIDITY -> {
                    unit = context.getString(unitsConverter.getHumidityUnit().unit)
                    sensorValue = tag.latestMeasurement.humidity?.valueWithoutUnit ?: ""
                }
                WidgetType.PRESSURE -> {
                    unit = context.getString(unitsConverter.getPressureUnit().unit)
                    sensorValue = tag.latestMeasurement.pressure?.valueWithoutUnit ?: ""
                }
                WidgetType.MOVEMENT -> {
                    unit = context.getString(R.string.movements)
                    sensorValue = tag.latestMeasurement.movement?.valueWithoutUnit ?: ""
                }
                WidgetType.VOLTAGE -> {
                    unit = context.getString(R.string.voltage_unit)
                    sensorValue = tag.latestMeasurement.voltage.valueWithoutUnit
                }
                WidgetType.SIGNAL_STRENGTH -> {
                    unit = context.getString(R.string.signal_unit)
                    sensorValue = tag.latestMeasurement.rssi.valueWithoutUnit
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
                WidgetType.AIR_QUALITY -> {
                    unit = tag.latestMeasurement.aqi?.unitString ?: ""
                    sensorValue = tag.latestMeasurement.aqi?.valueWithoutUnit ?: UNDEFINED_VALUE
                }
                WidgetType.LUMINOSITY -> {
                    unit = tag.latestMeasurement.luminosity?.unitString ?: ""
                    sensorValue = tag.latestMeasurement.luminosity?.valueWithoutUnit ?: UNDEFINED_VALUE
                }
                WidgetType.CO2 -> {
                    unit = tag.latestMeasurement.co2?.unitString ?: ""
                    sensorValue = tag.latestMeasurement.co2?.valueWithoutUnit ?: UNDEFINED_VALUE
                }
                WidgetType.VOC -> {
                    unit = tag.latestMeasurement.voc?.unitString ?: ""
                    sensorValue = tag.latestMeasurement.voc?.valueWithoutUnit ?: UNDEFINED_VALUE
                }
                WidgetType.NOX -> {
                    unit = tag.latestMeasurement.nox?.unitString ?: ""
                    sensorValue = tag.latestMeasurement.nox?.valueWithoutUnit ?: UNDEFINED_VALUE
                }
                WidgetType.PM10 -> {
                    unit = tag.latestMeasurement.pm10?.unitString ?: ""
                    sensorValue = tag.latestMeasurement.pm10?.valueWithoutUnit ?: UNDEFINED_VALUE
                }
                WidgetType.PM25 -> {
                    unit = tag.latestMeasurement.pm25?.unitString ?: ""
                    sensorValue = tag.latestMeasurement.pm25?.valueWithoutUnit ?: UNDEFINED_VALUE
                }
                WidgetType.PM40 -> {
                    unit = tag.latestMeasurement.pm40?.unitString ?: ""
                    sensorValue = tag.latestMeasurement.pm40?.valueWithoutUnit ?: UNDEFINED_VALUE
                }
                WidgetType.PM100 -> {
                    unit = tag.latestMeasurement.pm100?.unitString ?: ""
                    sensorValue = tag.latestMeasurement.pm100?.valueWithoutUnit ?: UNDEFINED_VALUE
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
                            context.getString(R.string.voltage_reading, decoded.voltage, "")
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
                    WidgetType.AIR_QUALITY -> {
                        unit = context.getString(UnitType.AirQuality.AqiIndex.unit)
                        sensorValue = AQI.getAQI(pm25 = decoded.pm25, co2 = decoded.co2).scoreString
                    }
                    WidgetType.LUMINOSITY -> {
                        unit = context.getString(UnitType.Luminosity.Lux.unit)
                        sensorValue = context.getString(UnitType.Luminosity.Lux.defaultAccuracy.nameTemplateId, decoded.luminosity, "");
                    }
                    WidgetType.CO2 -> {
                        unit = context.getString(UnitType.CO2.Ppm.unit)
                        sensorValue = context.getString(UnitType.CO2.Ppm.defaultAccuracy.nameTemplateId, decoded.co2, "");
                    }
                    WidgetType.VOC -> {
                        unit = context.getString(UnitType.VOC.VocIndex.unit)
                        sensorValue = context.getString(UnitType.VOC.VocIndex.defaultAccuracy.nameTemplateId, decoded.voc, "");
                    }
                    WidgetType.NOX -> {
                        unit = context.getString(UnitType.NOX.NoxIndex.unit)
                        sensorValue = context.getString(UnitType.NOX.NoxIndex.defaultAccuracy.nameTemplateId, decoded.nox, "");
                    }
                    WidgetType.PM10 -> {
                        unit = context.getString(UnitType.PM10.Mgm3.unit)
                        sensorValue = context.getString(UnitType.PM10.Mgm3.defaultAccuracy.nameTemplateId, decoded.pm1, "");
                    }
                    WidgetType.PM25 -> {
                        unit = context.getString(UnitType.PM25.Mgm3.unit)
                        sensorValue = context.getString(UnitType.PM25.Mgm3.defaultAccuracy.nameTemplateId, decoded.pm25, "");
                    }
                    WidgetType.PM40 -> {
                        unit = context.getString(UnitType.PM40.Mgm3.unit)
                        sensorValue = context.getString(UnitType.PM40.Mgm3.defaultAccuracy.nameTemplateId, decoded.pm4, "");
                    }
                    WidgetType.PM100 -> {
                        unit = context.getString(UnitType.PM100.Mgm3.unit)
                        sensorValue = context.getString(UnitType.PM100.Mgm3.defaultAccuracy.nameTemplateId, decoded.pm10, "");
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

    companion object {
        const val UNDEFINED_VALUE = "-"
    }
}