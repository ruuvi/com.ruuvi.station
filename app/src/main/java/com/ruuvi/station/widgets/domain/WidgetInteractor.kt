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
import com.ruuvi.station.units.domain.HumidityConverter
import com.ruuvi.station.units.domain.TemperatureConverter
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
                    sensorValue = unitsConverter.getTemperatureStringWithoutUnit(lastMeasurement.temperature, UnitType.TemperatureUnit.Celsius),
                    unit = context.getString(UnitType.TemperatureUnit.Celsius.unit)
                )

                val temperatureFValue = SensorValue(
                    type = WidgetType.TEMPERATURE_F,
                    sensorValue = unitsConverter.getTemperatureStringWithoutUnit(lastMeasurement.temperature, UnitType.TemperatureUnit.Fahrenheit),
                    unit = context.getString(UnitType.TemperatureUnit.Fahrenheit.unit)
                )

                val temperatureKValue = SensorValue(
                    type = WidgetType.TEMPERATURE_K,
                    sensorValue = unitsConverter.getTemperatureStringWithoutUnit(lastMeasurement.temperature, UnitType.TemperatureUnit.Kelvin),
                    unit = context.getString(UnitType.TemperatureUnit.Kelvin.unit)
                )

                val humidityValue = SensorValue(
                    type = WidgetType.HUMIDITY,
                    sensorValue = unitsConverter.getHumidityStringWithoutUnit(
                        lastMeasurement.humidity,
                        lastMeasurement.temperature ?: 0.0,
                        UnitType.HumidityUnit.Relative
                    ),
                    unit = context.getString(UnitType.HumidityUnit.Relative.unit)
                )

                val humidityAbsoluteValue = SensorValue(
                    type = WidgetType.HUMIDITY_ABSOLUTE,
                    sensorValue = unitsConverter.getHumidityStringWithoutUnit(
                        lastMeasurement.humidity,
                        lastMeasurement.temperature ?: 0.0,
                        UnitType.HumidityUnit.Absolute
                    ),
                    unit = context.getString(UnitType.HumidityUnit.Absolute.unit)
                )

                val dewPointCValue = SensorValue(
                    type = WidgetType.DEW_POINT_C,
                    sensorValue = formatDewPoint(lastMeasurement.humidity, lastMeasurement.temperature, UnitType.TemperatureUnit.Celsius),
                    unit = context.getString(UnitType.TemperatureUnit.Celsius.unit)
                )

                val dewPointFValue = SensorValue(
                    type = WidgetType.DEW_POINT_F,
                    sensorValue = formatDewPoint(lastMeasurement.humidity, lastMeasurement.temperature, UnitType.TemperatureUnit.Fahrenheit),
                    unit = context.getString(UnitType.TemperatureUnit.Fahrenheit.unit)
                )

                val dewPointKValue = SensorValue(
                    type = WidgetType.DEW_POINT_K,
                    sensorValue = formatDewPoint(lastMeasurement.humidity, lastMeasurement.temperature, UnitType.TemperatureUnit.Kelvin),
                    unit = context.getString(UnitType.TemperatureUnit.Kelvin.unit)
                )

                val pressureValue = SensorValue(
                    type = WidgetType.PRESSURE,
                    sensorValue = unitsConverter.getPressureStringWithoutUnit(lastMeasurement.pressure, UnitType.PressureUnit.HectoPascal),
                    unit = context.getString(UnitType.PressureUnit.HectoPascal.unit)
                )

                val pressurePaValue = SensorValue(
                    type = WidgetType.PRESSURE_PA,
                    sensorValue = unitsConverter.getPressureStringWithoutUnit(lastMeasurement.pressure, UnitType.PressureUnit.Pascal),
                    unit = context.getString(UnitType.PressureUnit.Pascal.unit)
                )

                val pressureMmHgValue = SensorValue(
                    type = WidgetType.PRESSURE_MMHG,
                    sensorValue = unitsConverter.getPressureStringWithoutUnit(lastMeasurement.pressure, UnitType.PressureUnit.MmHg),
                    unit = context.getString(UnitType.PressureUnit.MmHg.unit)
                )

                val pressureInHgValue = SensorValue(
                    type = WidgetType.PRESSURE_INHG,
                    sensorValue = unitsConverter.getPressureStringWithoutUnit(lastMeasurement.pressure, UnitType.PressureUnit.InchHg),
                    unit = context.getString(UnitType.PressureUnit.InchHg.unit)
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

                val soundAverageValue = SensorValue(
                    type = WidgetType.SOUND_AVERAGE,
                    sensorValue = sensorFav.latestMeasurement?.dBaAvg?.valueWithoutUnit ?: UNDEFINED_VALUE,
                    unit = sensorFav.latestMeasurement?.dBaAvg?.unitString
                        ?: context.getString(UnitType.SoundAvg.SoundDba.unit)
                )

                val soundRealTimeValue = SensorValue(
                    type = WidgetType.SOUND_REAL_TIME,
                    sensorValue = sensorFav.latestMeasurement?.dBaAvg?.valueWithoutUnit ?: UNDEFINED_VALUE,
                    unit = sensorFav.latestMeasurement?.dBaAvg?.unitString
                        ?: context.getString(UnitType.SoundAvg.SoundDba.unit)
                )

                val soundPeakValue = SensorValue(
                    type = WidgetType.SOUND_PEAK,
                    sensorValue = sensorFav.latestMeasurement?.dBaPeak?.valueWithoutUnit ?: UNDEFINED_VALUE,
                    unit = sensorFav.latestMeasurement?.dBaPeak?.unitString
                        ?: context.getString(UnitType.SoundPeak.SoundDba.unit)
                )

                val msnValue = SensorValue(
                    type = WidgetType.MEASUREMENT_SEQUENCE_NUMBER,
                    sensorValue = sensorFav.latestMeasurement?.measurementSequenceNumber?.toString() ?: UNDEFINED_VALUE,
                    unit = ""
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
                        context.getString(UnitType.PM.PM10.defaultAccuracy.nameTemplateId, it, "")
                    } ?: UNDEFINED_VALUE,
                    unit = context.getString(UnitType.PM.PM10.unit)
                )

                val pm25Value = SensorValue(
                    type = WidgetType.PM25,
                    sensorValue = lastMeasurement.pm25?.let {
                        context.getString(UnitType.PM.PM25.defaultAccuracy.nameTemplateId, it, "")
                    } ?: UNDEFINED_VALUE,
                    unit = context.getString(UnitType.PM.PM25.unit)
                )

                val pm40Value = SensorValue(
                    type = WidgetType.PM40,
                    sensorValue = lastMeasurement.pm4?.let {
                        context.getString(UnitType.PM.PM40.defaultAccuracy.nameTemplateId, it, "")
                    } ?: UNDEFINED_VALUE,
                    unit = context.getString(UnitType.PM.PM40.unit)
                )

                val pm100Value = SensorValue(
                    type = WidgetType.PM100,
                    sensorValue = lastMeasurement.pm10?.let {
                        context.getString(UnitType.PM.PM100.defaultAccuracy.nameTemplateId, it, "")
                    } ?: UNDEFINED_VALUE,
                    unit = context.getString(UnitType.PM.PM100.unit)
                )

                val co2Value = SensorValue(
                    type = WidgetType.CO2,
                    sensorValue = lastMeasurement.co2?.let {
                        context.getString(UnitType.CO2.Ppm.defaultAccuracy.nameTemplateId, it.toDouble(), "")
                    } ?: UNDEFINED_VALUE,
                    unit = context.getString(UnitType.CO2.Ppm.unit)
                )

                val vocValue = SensorValue(
                    type = WidgetType.VOC,
                    sensorValue = lastMeasurement.voc?.let {
                        context.getString(UnitType.VOC.VocIndex.defaultAccuracy.nameTemplateId, it.toDouble(), "")
                    } ?: UNDEFINED_VALUE,
                    unit = context.getString(UnitType.VOC.VocIndex.unit)
                )

                val noxValue = SensorValue(
                    type = WidgetType.NOX,
                    sensorValue = lastMeasurement.nox?.let {
                        context.getString(UnitType.NOX.NoxIndex.defaultAccuracy.nameTemplateId, it.toDouble(), "")
                    } ?: UNDEFINED_VALUE,
                    unit = context.getString(UnitType.NOX.NoxIndex.unit)
                )


                result.updated = if (lastMeasurement.updatedAt.diffGreaterThan(hours24)) {
                    lastMeasurement.updatedAt.localizedDate(context)
                } else {
                    lastMeasurement.updatedAt.localizedTime(context)
                }

                val sensorValuesByType = mapOf(
                    WidgetType.AIR_QUALITY to aqiValue,
                    WidgetType.TEMPERATURE to temperatureValue,
                    WidgetType.TEMPERATURE_F to temperatureFValue,
                    WidgetType.TEMPERATURE_K to temperatureKValue,
                    WidgetType.HUMIDITY to humidityValue,
                    WidgetType.HUMIDITY_ABSOLUTE to humidityAbsoluteValue,
                    WidgetType.DEW_POINT_C to dewPointCValue,
                    WidgetType.DEW_POINT_F to dewPointFValue,
                    WidgetType.DEW_POINT_K to dewPointKValue,
                    WidgetType.PRESSURE to pressureValue,
                    WidgetType.PRESSURE_PA to pressurePaValue,
                    WidgetType.PRESSURE_MMHG to pressureMmHgValue,
                    WidgetType.PRESSURE_INHG to pressureInHgValue,
                    WidgetType.MOVEMENT to movementsValue,
                    WidgetType.VOLTAGE to voltageValue,
                    WidgetType.SIGNAL_STRENGTH to signalStrengthValue,
                    WidgetType.ACCELERATION_X to accelerationXValue,
                    WidgetType.ACCELERATION_Y to accelerationYValue,
                    WidgetType.ACCELERATION_Z to accelerationZValue,
                    WidgetType.SOUND_REAL_TIME to soundRealTimeValue,
                    WidgetType.SOUND_AVERAGE to soundAverageValue,
                    WidgetType.SOUND_PEAK to soundPeakValue,
                    WidgetType.MEASUREMENT_SEQUENCE_NUMBER to msnValue,
                    WidgetType.LUMINOSITY to luminosityValue,
                    WidgetType.PM10 to pm10Value,
                    WidgetType.PM25 to pm25Value,
                    WidgetType.PM40 to pm40Value,
                    WidgetType.PM100 to pm100Value,
                    WidgetType.CO2 to co2Value,
                    WidgetType.VOC to vocValue,
                    WidgetType.NOX to noxValue,
                )
            result.sensorValues = WidgetType.filterWidgetTypes(sensorFav)
                .filter { isTypeChecked(settings, it) }
                .mapNotNull { sensorValuesByType[it] }
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
                sensorValue = unitsConverter.getTemperatureStringWithoutUnit(lastMeasurement.temperature?.original, UnitType.TemperatureUnit.Celsius),
                unit = context.getString(UnitType.TemperatureUnit.Celsius.unit)
            )

            val temperatureFValue = SensorValue(
                type = WidgetType.TEMPERATURE_F,
                sensorValue = unitsConverter.getTemperatureStringWithoutUnit(lastMeasurement.temperature?.original, UnitType.TemperatureUnit.Fahrenheit),
                unit = context.getString(UnitType.TemperatureUnit.Fahrenheit.unit)
            )

            val temperatureKValue = SensorValue(
                type = WidgetType.TEMPERATURE_K,
                sensorValue = unitsConverter.getTemperatureStringWithoutUnit(lastMeasurement.temperature?.original, UnitType.TemperatureUnit.Kelvin),
                unit = context.getString(UnitType.TemperatureUnit.Kelvin.unit)
            )

            val humidityValue = SensorValue(
                type = WidgetType.HUMIDITY,
                sensorValue = unitsConverter.getHumidityStringWithoutUnit(
                    lastMeasurement.humidity?.original,
                    lastMeasurement.temperature?.original,
                    UnitType.HumidityUnit.Relative
                ),
                unit = context.getString(UnitType.HumidityUnit.Relative.unit)
            )

            val humidityAbsoluteValue = SensorValue(
                type = WidgetType.HUMIDITY_ABSOLUTE,
                sensorValue = unitsConverter.getHumidityStringWithoutUnit(
                    lastMeasurement.humidity?.original,
                    lastMeasurement.temperature?.original,
                    UnitType.HumidityUnit.Absolute
                ),
                unit = context.getString(UnitType.HumidityUnit.Absolute.unit)
            )

            val dewPointCValue = SensorValue(
                type = WidgetType.DEW_POINT_C,
                sensorValue = formatDewPoint(lastMeasurement.humidity?.original, lastMeasurement.temperature?.original, UnitType.TemperatureUnit.Celsius),
                unit = context.getString(UnitType.TemperatureUnit.Celsius.unit)
            )

            val dewPointFValue = SensorValue(
                type = WidgetType.DEW_POINT_F,
                sensorValue = formatDewPoint(lastMeasurement.humidity?.original, lastMeasurement.temperature?.original, UnitType.TemperatureUnit.Fahrenheit),
                unit = context.getString(UnitType.TemperatureUnit.Fahrenheit.unit)
            )

            val dewPointKValue = SensorValue(
                type = WidgetType.DEW_POINT_K,
                sensorValue = formatDewPoint(lastMeasurement.humidity?.original, lastMeasurement.temperature?.original, UnitType.TemperatureUnit.Kelvin),
                unit = context.getString(UnitType.TemperatureUnit.Kelvin.unit)
            )

            val pressureValue = SensorValue(
                type = WidgetType.PRESSURE,
                sensorValue = unitsConverter.getPressureStringWithoutUnit(lastMeasurement.pressure?.original, UnitType.PressureUnit.HectoPascal),
                unit = context.getString(UnitType.PressureUnit.HectoPascal.unit)
            )

            val pressurePaValue = SensorValue(
                type = WidgetType.PRESSURE_PA,
                sensorValue = unitsConverter.getPressureStringWithoutUnit(lastMeasurement.pressure?.original, UnitType.PressureUnit.Pascal),
                unit = context.getString(UnitType.PressureUnit.Pascal.unit)
            )

            val pressureMmHgValue = SensorValue(
                type = WidgetType.PRESSURE_MMHG,
                sensorValue = unitsConverter.getPressureStringWithoutUnit(lastMeasurement.pressure?.original, UnitType.PressureUnit.MmHg),
                unit = context.getString(UnitType.PressureUnit.MmHg.unit)
            )

            val pressureInHgValue = SensorValue(
                type = WidgetType.PRESSURE_INHG,
                sensorValue = unitsConverter.getPressureStringWithoutUnit(lastMeasurement.pressure?.original, UnitType.PressureUnit.InchHg),
                unit = context.getString(UnitType.PressureUnit.InchHg.unit)
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

            val soundAverageValue = SensorValue(
                type = WidgetType.SOUND_AVERAGE,
                sensorValue = lastMeasurement.dBaAvg?.valueWithoutUnit ?: UNDEFINED_VALUE,
                unit = lastMeasurement.dBaAvg?.unitString ?: context.getString(UnitType.SoundAvg.SoundDba.unit)
            )

            val soundRealTimeValue = SensorValue(
                type = WidgetType.SOUND_REAL_TIME,
                sensorValue = lastMeasurement.dBaAvg?.valueWithoutUnit ?: UNDEFINED_VALUE,
                unit = lastMeasurement.dBaAvg?.unitString ?: context.getString(UnitType.SoundAvg.SoundDba.unit)
            )

            val soundPeakValue = SensorValue(
                type = WidgetType.SOUND_PEAK,
                sensorValue = lastMeasurement.dBaPeak?.valueWithoutUnit ?: UNDEFINED_VALUE,
                unit = lastMeasurement.dBaPeak?.unitString ?: context.getString(UnitType.SoundPeak.SoundDba.unit)
            )

            val msnValue = SensorValue(
                type = WidgetType.MEASUREMENT_SEQUENCE_NUMBER,
                sensorValue = lastMeasurement.measurementSequenceNumber.toString(),
                unit = ""
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
                    context.getString(UnitType.PM.PM10.defaultAccuracy.nameTemplateId, it.value, "")
                } ?: UNDEFINED_VALUE,
                unit = context.getString(UnitType.PM.PM10.unit)
            )

            val pm25Value = SensorValue(
                type = WidgetType.PM25,
                sensorValue = lastMeasurement.pm25?.let {
                    context.getString(UnitType.PM.PM25.defaultAccuracy.nameTemplateId, it.value, "")
                } ?: UNDEFINED_VALUE,
                unit = context.getString(UnitType.PM.PM25.unit)
            )

            val pm40Value = SensorValue(
                type = WidgetType.PM40,
                sensorValue = lastMeasurement.pm40?.let {
                    context.getString(UnitType.PM.PM40.defaultAccuracy.nameTemplateId, it.value, "")
                } ?: UNDEFINED_VALUE,
                unit = context.getString(UnitType.PM.PM40.unit)
            )

            val pm100Value = SensorValue(
                type = WidgetType.PM100,
                sensorValue = lastMeasurement.pm100?.let {
                    context.getString(UnitType.PM.PM100.defaultAccuracy.nameTemplateId, it.value, "")
                } ?: UNDEFINED_VALUE,
                unit = context.getString(UnitType.PM.PM100.unit)
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

            val sensorValuesByType = mapOf(
                WidgetType.AIR_QUALITY to aqiValue,
                WidgetType.TEMPERATURE to temperatureValue,
                WidgetType.TEMPERATURE_F to temperatureFValue,
                WidgetType.TEMPERATURE_K to temperatureKValue,
                WidgetType.HUMIDITY to humidityValue,
                WidgetType.HUMIDITY_ABSOLUTE to humidityAbsoluteValue,
                WidgetType.DEW_POINT_C to dewPointCValue,
                WidgetType.DEW_POINT_F to dewPointFValue,
                WidgetType.DEW_POINT_K to dewPointKValue,
                WidgetType.PRESSURE to pressureValue,
                WidgetType.PRESSURE_PA to pressurePaValue,
                WidgetType.PRESSURE_MMHG to pressureMmHgValue,
                WidgetType.PRESSURE_INHG to pressureInHgValue,
                WidgetType.MOVEMENT to movementsValue,
                WidgetType.VOLTAGE to voltageValue,
                WidgetType.SIGNAL_STRENGTH to signalStrengthValue,
                WidgetType.ACCELERATION_X to accelerationXValue,
                WidgetType.ACCELERATION_Y to accelerationYValue,
                WidgetType.ACCELERATION_Z to accelerationZValue,
                WidgetType.SOUND_REAL_TIME to soundRealTimeValue,
                WidgetType.SOUND_AVERAGE to soundAverageValue,
                WidgetType.SOUND_PEAK to soundPeakValue,
                WidgetType.MEASUREMENT_SEQUENCE_NUMBER to msnValue,
                WidgetType.LUMINOSITY to luminosityValue,
                WidgetType.PM10 to pm10Value,
                WidgetType.PM25 to pm25Value,
                WidgetType.PM40 to pm40Value,
                WidgetType.PM100 to pm100Value,
                WidgetType.CO2 to co2Value,
                WidgetType.VOC to vocValue,
                WidgetType.NOX to noxValue,
            )
            result.sensorValues = WidgetType.filterWidgetTypes(sensorFav)
                .filter { isTypeChecked(settings, it) }
                .mapNotNull { sensorValuesByType[it] }
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
                    unit = context.getString(UnitType.TemperatureUnit.Celsius.unit)
                    sensorValue = unitsConverter.getTemperatureStringWithoutUnit(
                        tag.latestMeasurement.temperature?.original,
                        UnitType.TemperatureUnit.Celsius
                    )
                }
                WidgetType.TEMPERATURE_F -> {
                    unit = context.getString(UnitType.TemperatureUnit.Fahrenheit.unit)
                    sensorValue = unitsConverter.getTemperatureStringWithoutUnit(
                        tag.latestMeasurement.temperature?.original,
                        UnitType.TemperatureUnit.Fahrenheit
                    )
                }
                WidgetType.TEMPERATURE_K -> {
                    unit = context.getString(UnitType.TemperatureUnit.Kelvin.unit)
                    sensorValue = unitsConverter.getTemperatureStringWithoutUnit(
                        tag.latestMeasurement.temperature?.original,
                        UnitType.TemperatureUnit.Kelvin
                    )
                }
                WidgetType.HUMIDITY -> {
                    unit = context.getString(UnitType.HumidityUnit.Relative.unit)
                    sensorValue = unitsConverter.getHumidityStringWithoutUnit(
                        tag.latestMeasurement.humidity?.original,
                        tag.latestMeasurement.temperature?.original,
                        UnitType.HumidityUnit.Relative
                    )
                }
                WidgetType.HUMIDITY_ABSOLUTE -> {
                    unit = context.getString(UnitType.HumidityUnit.Absolute.unit)
                    sensorValue = unitsConverter.getHumidityStringWithoutUnit(
                        tag.latestMeasurement.humidity?.original,
                        tag.latestMeasurement.temperature?.original,
                        UnitType.HumidityUnit.Absolute
                    )
                }
                WidgetType.DEW_POINT_C -> {
                    unit = context.getString(UnitType.TemperatureUnit.Celsius.unit)
                    sensorValue = formatDewPoint(
                        humidity = tag.latestMeasurement.humidity?.original,
                        temperature = tag.latestMeasurement.temperature?.original,
                        unit = UnitType.TemperatureUnit.Celsius
                    )
                }
                WidgetType.DEW_POINT_F -> {
                    unit = context.getString(UnitType.TemperatureUnit.Fahrenheit.unit)
                    sensorValue = formatDewPoint(
                        humidity = tag.latestMeasurement.humidity?.original,
                        temperature = tag.latestMeasurement.temperature?.original,
                        unit = UnitType.TemperatureUnit.Fahrenheit
                    )
                }
                WidgetType.DEW_POINT_K -> {
                    unit = context.getString(UnitType.TemperatureUnit.Kelvin.unit)
                    sensorValue = formatDewPoint(
                        humidity = tag.latestMeasurement.humidity?.original,
                        temperature = tag.latestMeasurement.temperature?.original,
                        unit = UnitType.TemperatureUnit.Kelvin
                    )
                }
                WidgetType.PRESSURE -> {
                    unit = context.getString(UnitType.PressureUnit.HectoPascal.unit)
                    sensorValue = unitsConverter.getPressureStringWithoutUnit(
                        tag.latestMeasurement.pressure?.original,
                        UnitType.PressureUnit.HectoPascal
                    )
                }
                WidgetType.PRESSURE_PA -> {
                    unit = context.getString(UnitType.PressureUnit.Pascal.unit)
                    sensorValue = unitsConverter.getPressureStringWithoutUnit(
                        tag.latestMeasurement.pressure?.original,
                        UnitType.PressureUnit.Pascal
                    )
                }
                WidgetType.PRESSURE_MMHG -> {
                    unit = context.getString(UnitType.PressureUnit.MmHg.unit)
                    sensorValue = unitsConverter.getPressureStringWithoutUnit(
                        tag.latestMeasurement.pressure?.original,
                        UnitType.PressureUnit.MmHg
                    )
                }
                WidgetType.PRESSURE_INHG -> {
                    unit = context.getString(UnitType.PressureUnit.InchHg.unit)
                    sensorValue = unitsConverter.getPressureStringWithoutUnit(
                        tag.latestMeasurement.pressure?.original,
                        UnitType.PressureUnit.InchHg
                    )
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
                WidgetType.SOUND_REAL_TIME -> {
                    unit = tag.latestMeasurement.dBaAvg?.unitString
                        ?: context.getString(UnitType.SoundAvg.SoundDba.unit)
                    sensorValue = tag.latestMeasurement.dBaAvg?.valueWithoutUnit ?: UNDEFINED_VALUE
                }
                WidgetType.SOUND_AVERAGE -> {
                    unit = tag.latestMeasurement.dBaAvg?.unitString
                        ?: context.getString(UnitType.SoundAvg.SoundDba.unit)
                    sensorValue = tag.latestMeasurement.dBaAvg?.valueWithoutUnit ?: UNDEFINED_VALUE
                }
                WidgetType.SOUND_PEAK -> {
                    unit = tag.latestMeasurement.dBaPeak?.unitString
                        ?: context.getString(UnitType.SoundPeak.SoundDba.unit)
                    sensorValue = tag.latestMeasurement.dBaPeak?.valueWithoutUnit ?: UNDEFINED_VALUE
                }
                WidgetType.MEASUREMENT_SEQUENCE_NUMBER -> {
                    unit = ""
                    sensorValue = tag.latestMeasurement.measurementSequenceNumber.toString()
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
                measurementName = context.getString(widgetType.titleResId),
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
                        unit = context.getString(UnitType.TemperatureUnit.Celsius.unit)
                        sensorValue =
                            unitsConverter.getTemperatureStringWithoutUnit(
                                decoded.temperature,
                                UnitType.TemperatureUnit.Celsius
                            )
                    }
                    WidgetType.TEMPERATURE_F -> {
                        unit = context.getString(UnitType.TemperatureUnit.Fahrenheit.unit)
                        sensorValue = unitsConverter.getTemperatureStringWithoutUnit(
                            decoded.temperature,
                            UnitType.TemperatureUnit.Fahrenheit
                        )
                    }
                    WidgetType.TEMPERATURE_K -> {
                        unit = context.getString(UnitType.TemperatureUnit.Kelvin.unit)
                        sensorValue = unitsConverter.getTemperatureStringWithoutUnit(
                            decoded.temperature,
                            UnitType.TemperatureUnit.Kelvin
                        )
                    }
                    WidgetType.HUMIDITY -> {
                        unit = context.getString(UnitType.HumidityUnit.Relative.unit)
                        sensorValue = unitsConverter.getHumidityStringWithoutUnit(
                            decoded.humidity,
                            decoded.temperature ?: 0.0,
                            UnitType.HumidityUnit.Relative
                        )
                    }
                    WidgetType.HUMIDITY_ABSOLUTE -> {
                        unit = context.getString(UnitType.HumidityUnit.Absolute.unit)
                        sensorValue = unitsConverter.getHumidityStringWithoutUnit(
                            decoded.humidity,
                            decoded.temperature ?: 0.0,
                            UnitType.HumidityUnit.Absolute
                        )
                    }
                    WidgetType.DEW_POINT_C -> {
                        unit = context.getString(UnitType.TemperatureUnit.Celsius.unit)
                        sensorValue = formatDewPoint(decoded.humidity, decoded.temperature, UnitType.TemperatureUnit.Celsius)
                    }
                    WidgetType.DEW_POINT_F -> {
                        unit = context.getString(UnitType.TemperatureUnit.Fahrenheit.unit)
                        sensorValue = formatDewPoint(decoded.humidity, decoded.temperature, UnitType.TemperatureUnit.Fahrenheit)
                    }
                    WidgetType.DEW_POINT_K -> {
                        unit = context.getString(UnitType.TemperatureUnit.Kelvin.unit)
                        sensorValue = formatDewPoint(decoded.humidity, decoded.temperature, UnitType.TemperatureUnit.Kelvin)
                    }
                    WidgetType.PRESSURE -> {
                        unit = context.getString(UnitType.PressureUnit.HectoPascal.unit)
                        sensorValue = unitsConverter.getPressureStringWithoutUnit(decoded.pressure, UnitType.PressureUnit.HectoPascal)
                    }
                    WidgetType.PRESSURE_PA -> {
                        unit = context.getString(UnitType.PressureUnit.Pascal.unit)
                        sensorValue = unitsConverter.getPressureStringWithoutUnit(decoded.pressure, UnitType.PressureUnit.Pascal)
                    }
                    WidgetType.PRESSURE_MMHG -> {
                        unit = context.getString(UnitType.PressureUnit.MmHg.unit)
                        sensorValue = unitsConverter.getPressureStringWithoutUnit(decoded.pressure, UnitType.PressureUnit.MmHg)
                    }
                    WidgetType.PRESSURE_INHG -> {
                        unit = context.getString(UnitType.PressureUnit.InchHg.unit)
                        sensorValue = unitsConverter.getPressureStringWithoutUnit(decoded.pressure, UnitType.PressureUnit.InchHg)
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
                    WidgetType.SOUND_REAL_TIME -> {
                        unit = tag.latestMeasurement?.dBaAvg?.unitString
                            ?: context.getString(UnitType.SoundAvg.SoundDba.unit)
                        sensorValue = tag.latestMeasurement?.dBaAvg?.valueWithoutUnit ?: UNDEFINED_VALUE
                    }
                    WidgetType.SOUND_AVERAGE -> {
                        unit = tag.latestMeasurement?.dBaAvg?.unitString
                            ?: context.getString(UnitType.SoundAvg.SoundDba.unit)
                        sensorValue = tag.latestMeasurement?.dBaAvg?.valueWithoutUnit ?: UNDEFINED_VALUE
                    }
                    WidgetType.SOUND_PEAK -> {
                        unit = tag.latestMeasurement?.dBaPeak?.unitString
                            ?: context.getString(UnitType.SoundPeak.SoundDba.unit)
                        sensorValue = tag.latestMeasurement?.dBaPeak?.valueWithoutUnit ?: UNDEFINED_VALUE
                    }
                    WidgetType.MEASUREMENT_SEQUENCE_NUMBER -> {
                        unit = ""
                        sensorValue = tag.latestMeasurement?.measurementSequenceNumber?.toString() ?: UNDEFINED_VALUE
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
                        unit = context.getString(UnitType.PM.PM10.unit)
                        sensorValue = context.getString(UnitType.PM.PM10.defaultAccuracy.nameTemplateId, decoded.pm1, "");
                    }
                    WidgetType.PM25 -> {
                        unit = context.getString(UnitType.PM.PM25.unit)
                        sensorValue = context.getString(UnitType.PM.PM25.defaultAccuracy.nameTemplateId, decoded.pm25, "");
                    }
                    WidgetType.PM40 -> {
                        unit = context.getString(UnitType.PM.PM40.unit)
                        sensorValue = context.getString(UnitType.PM.PM40.defaultAccuracy.nameTemplateId, decoded.pm4, "");
                    }
                    WidgetType.PM100 -> {
                        unit = context.getString(UnitType.PM.PM100.unit)
                        sensorValue = context.getString(UnitType.PM.PM100.defaultAccuracy.nameTemplateId, decoded.pm10, "");
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
                    measurementName = context.getString(widgetType.titleResId),
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

    private fun formatDewPoint(
        humidity: Double?,
        temperature: Double?,
        unit: UnitType.TemperatureUnit
    ): String {
        if (humidity == null || temperature == null || temperature !in -100.0..370.0) {
            return UNDEFINED_VALUE
        }
        val converter = HumidityConverter(temperature, humidity / 100)
        val dewCelsius = converter.toDewCelsius ?: return UNDEFINED_VALUE
        val converted = when (unit) {
            UnitType.TemperatureUnit.Celsius -> dewCelsius
            UnitType.TemperatureUnit.Fahrenheit -> TemperatureConverter.celsiusToFahrenheit(dewCelsius)
            UnitType.TemperatureUnit.Kelvin -> TemperatureConverter.celsiusToKelvin(dewCelsius)
        }
        return unitsConverter.getValueWithoutUnit(converted, unitsConverter.getHumidityAccuracy())
    }

    private fun isTypeChecked(settings: ComplexWidgetPreferenceItem?, type: WidgetType): Boolean {
        if (settings == null) return false
        return when (type) {
            WidgetType.TEMPERATURE -> settings.checkedTemperature
            WidgetType.TEMPERATURE_F -> settings.checkedTemperatureF
            WidgetType.TEMPERATURE_K -> settings.checkedTemperatureK
            WidgetType.HUMIDITY -> settings.checkedHumidity
            WidgetType.HUMIDITY_ABSOLUTE -> settings.checkedHumidityAbsolute
            WidgetType.DEW_POINT_C -> settings.checkedDewPointC
            WidgetType.DEW_POINT_F -> settings.checkedDewPointF
            WidgetType.DEW_POINT_K -> settings.checkedDewPointK
            WidgetType.PRESSURE -> settings.checkedPressure
            WidgetType.PRESSURE_PA -> settings.checkedPressurePa
            WidgetType.PRESSURE_MMHG -> settings.checkedPressureMmHg
            WidgetType.PRESSURE_INHG -> settings.checkedPressureInHg
            WidgetType.MOVEMENT -> settings.checkedMovement
            WidgetType.VOLTAGE -> settings.checkedVoltage
            WidgetType.SIGNAL_STRENGTH -> settings.checkedSignalStrength
            WidgetType.ACCELERATION_X -> settings.checkedAccelerationX
            WidgetType.ACCELERATION_Y -> settings.checkedAccelerationY
            WidgetType.ACCELERATION_Z -> settings.checkedAccelerationZ
            WidgetType.SOUND_REAL_TIME -> settings.checkedSoundRealTime
            WidgetType.SOUND_AVERAGE -> settings.checkedSoundAverage
            WidgetType.SOUND_PEAK -> settings.checkedSoundPeak
            WidgetType.MEASUREMENT_SEQUENCE_NUMBER -> settings.checkedMsn
            WidgetType.AIR_QUALITY -> settings.checkedAQI
            WidgetType.LUMINOSITY -> settings.checkedLuminosity
            WidgetType.CO2 -> settings.checkedCO2
            WidgetType.VOC -> settings.checkedVOC
            WidgetType.NOX -> settings.checkedNOX
            WidgetType.PM10 -> settings.checkedPM10
            WidgetType.PM25 -> settings.checkedPM25
            WidgetType.PM40 -> settings.checkedPM40
            WidgetType.PM100 -> settings.checkedPM100
        }
    }

    private fun emptySimpleResult(sensorId: String): SimpleWidgetData = SimpleWidgetData(sensorId, Date(0), context.getString(R.string.no_data), "", "", "", null)

    fun emptyComplexResult(sensorId: String): ComplexWidgetData = ComplexWidgetData(sensorId, Date(0), context.getString(R.string.no_data), emptyList(), null)

    private fun isCloudSensor(sensor: RuuviTag) = sensor.networkLastSync != null

    companion object {
        const val UNDEFINED_VALUE = "-"
    }
}
