package com.ruuvi.station.widgets.domain

import android.content.Context
import com.ruuvi.station.units.domain.AccelerationConverter
import com.ruuvi.station.units.domain.HumidityConverter
import com.ruuvi.station.units.domain.TemperatureConverter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.widgets.data.SensorValue
import com.ruuvi.station.widgets.data.WidgetSensorSnapshot
import com.ruuvi.station.widgets.data.WidgetType

private typealias WidgetValueFormatter = (WidgetSensorSnapshot) -> String

/**
 * Canonical measurement formatter shared by simple and complex widgets.
 *
 * Source selection and measurement ordering are intentionally kept outside this
 * class. Formatting an [Iterable] preserves the order supplied by the caller.
 */
internal class WidgetMeasurementFormatterRegistry(
    private val context: Context,
    private val unitsConverter: UnitsConverter,
    private val accelerationConverter: AccelerationConverter,
) {
    private val valueFormatters: Map<WidgetType, WidgetValueFormatter> = linkedMapOf(
        WidgetType.TEMPERATURE to temperatureFormatter(UnitType.TemperatureUnit.Celsius),
        WidgetType.HUMIDITY to relativeHumidityFormatter(),
        WidgetType.PRESSURE to pressureFormatter(UnitType.PressureUnit.HectoPascal),
        WidgetType.MOVEMENT to integerFormatter(value = { it.movementCount }),
        WidgetType.VOLTAGE to finiteValueFormatter(
            value = { it.voltageVolt },
            format = { unitsConverter.getVoltageEnvironmentValue(it).valueWithoutUnit },
        ),
        WidgetType.SIGNAL_STRENGTH to integerFormatter(
            value = { it.rssiDbm },
            format = { unitsConverter.getSignalEnvironmentValue(it).valueWithoutUnit },
        ),
        WidgetType.ACCELERATION_X to accelerationFormatter(
            value = { it.accelerationXG },
        ),
        WidgetType.ACCELERATION_Y to accelerationFormatter(
            value = { it.accelerationYG },
        ),
        WidgetType.ACCELERATION_Z to accelerationFormatter(
            value = { it.accelerationZG },
        ),
        WidgetType.SOUND_AVERAGE to soundFormatter(
            unitType = UnitType.SoundAvg.SoundDba,
            value = { it.soundAverageDba },
        ),
        WidgetType.SOUND_PEAK to soundFormatter(
            unitType = UnitType.SoundPeak.SoundDba,
            value = { it.soundPeakDba },
        ),
        WidgetType.MEASUREMENT_SEQUENCE_NUMBER to integerFormatter(
            value = { it.measurementSequenceNumber },
        ),
        WidgetType.TEMPERATURE_F to temperatureFormatter(UnitType.TemperatureUnit.Fahrenheit),
        WidgetType.TEMPERATURE_K to temperatureFormatter(UnitType.TemperatureUnit.Kelvin),
        WidgetType.HUMIDITY_ABSOLUTE to absoluteHumidityFormatter(),
        WidgetType.DEW_POINT_C to dewPointFormatter(UnitType.TemperatureUnit.Celsius),
        WidgetType.DEW_POINT_F to dewPointFormatter(UnitType.TemperatureUnit.Fahrenheit),
        WidgetType.DEW_POINT_K to dewPointFormatter(UnitType.TemperatureUnit.Kelvin),
        WidgetType.PRESSURE_PA to pressureFormatter(UnitType.PressureUnit.Pascal),
        WidgetType.PRESSURE_MMHG to pressureFormatter(UnitType.PressureUnit.MmHg),
        WidgetType.AIR_QUALITY to airQualityFormatter(),
        WidgetType.LUMINOSITY to finiteValueFormatter(
            value = { it.luminosityLux },
            format = { unitsConverter.getLuminosityEnvironmentValue(it).valueWithoutUnit },
        ),
        WidgetType.CO2 to integerFormatter(
            value = { it.co2Ppm },
            format = { unitsConverter.getCo2EnvironmentValue(it).valueWithoutUnit },
        ),
        WidgetType.NOX to integerFormatter(
            value = { it.noxIndex },
            format = { unitsConverter.getNoxEnvironmentValue(it).valueWithoutUnit },
        ),
        // Persisted PM codes use PM10 for PM1.0 and PM100 for PM10.
        WidgetType.PM10 to particulateFormatter(
            unitType = UnitType.PM.PM10,
            value = { it.pm1MicrogramsPerCubicMeter },
        ),
        WidgetType.PM25 to particulateFormatter(
            unitType = UnitType.PM.PM25,
            value = { it.pm2_5MicrogramsPerCubicMeter },
        ),
        WidgetType.PM40 to particulateFormatter(
            unitType = UnitType.PM.PM40,
            value = { it.pm4MicrogramsPerCubicMeter },
        ),
        WidgetType.PM100 to particulateFormatter(
            unitType = UnitType.PM.PM100,
            value = { it.pm10MicrogramsPerCubicMeter },
        ),
        WidgetType.VOC to integerFormatter(
            value = { it.vocIndex },
            format = { unitsConverter.getVocEnvironmentValue(it).valueWithoutUnit },
        ),
        WidgetType.PRESSURE_INHG to pressureFormatter(UnitType.PressureUnit.InchHg),
    )

    init {
        val registeredTypes = valueFormatters.keys
        val expectedTypes = WidgetType.entries.toSet()
        check(registeredTypes == expectedTypes) {
            val missing = expectedTypes - registeredTypes
            val unexpected = registeredTypes - expectedTypes
            "Widget formatter registration mismatch. Missing=$missing, unexpected=$unexpected"
        }
    }

    fun format(type: WidgetType, snapshot: WidgetSensorSnapshot): SensorValue {
        val value = valueFormatters.getValue(type)(snapshot).canonicalValue()
        return SensorValue(
            type = type,
            sensorValue = value,
            unit = context.getString(unitResource(type)).trim(),
        )
    }

    fun format(
        types: Iterable<WidgetType>,
        snapshot: WidgetSensorSnapshot,
    ): List<SensorValue> = types.map { format(it, snapshot) }

    private fun temperatureFormatter(
        unit: UnitType.TemperatureUnit,
    ): WidgetValueFormatter = { snapshot ->
        unitsConverter.getTemperatureStringWithoutUnit(
            temperature = snapshot.temperatureCelsius.finiteOrNull(),
            temperatureUnit = unit,
        )
    }

    private fun relativeHumidityFormatter(): WidgetValueFormatter = { snapshot ->
        unitsConverter.getHumidityStringWithoutUnit(
            humidity = snapshot.relativeHumidityPercent.finiteOrNull(),
            temperature = snapshot.temperatureCelsius.finiteOrNull(),
            humidityUnit = UnitType.HumidityUnit.Relative,
        )
    }

    private fun absoluteHumidityFormatter(): WidgetValueFormatter = { snapshot ->
        val temperature = snapshot.temperatureCelsius.finiteOrNull()
        val humidity = snapshot.relativeHumidityPercent.finiteOrNull()
            ?.takeIf { it in 0.0..MAX_RELATIVE_HUMIDITY_PERCENT }
        if (temperature == null || humidity == null) {
            UNAVAILABLE_VALUE
        } else {
            unitsConverter.getHumidityStringWithoutUnit(
                humidity = humidity,
                temperature = temperature,
                humidityUnit = UnitType.HumidityUnit.Absolute,
            )
        }
    }

    private fun dewPointFormatter(
        unit: UnitType.TemperatureUnit,
    ): WidgetValueFormatter = { snapshot ->
        formatDewPoint(
            humidityPercent = snapshot.relativeHumidityPercent,
            temperatureCelsius = snapshot.temperatureCelsius,
            unit = unit,
        )
    }

    private fun pressureFormatter(
        unit: UnitType.PressureUnit,
    ): WidgetValueFormatter = { snapshot ->
        unitsConverter.getPressureStringWithoutUnit(
            pressure = snapshot.pressurePascal.finiteOrNull(),
            pressureUnit = unit,
        )
    }

    private fun accelerationFormatter(
        value: (WidgetSensorSnapshot) -> Double?,
    ): WidgetValueFormatter = finiteValueFormatter(value) {
        accelerationConverter.getAccelerationStringWithoutUnit(it)
    }

    private fun soundFormatter(
        unitType: UnitType,
        value: (WidgetSensorSnapshot) -> Double?,
    ): WidgetValueFormatter = finiteValueFormatter(value) {
        unitsConverter.getSoundEnvironmentValue(it, unitType).valueWithoutUnit
    }

    private fun particulateFormatter(
        unitType: UnitType,
        value: (WidgetSensorSnapshot) -> Double?,
    ): WidgetValueFormatter = finiteValueFormatter(value) {
        unitsConverter.getPmEnvironmentValue(it, unitType).valueWithoutUnit
    }

    private fun airQualityFormatter(): WidgetValueFormatter = { snapshot ->
        val pm25 = snapshot.pm2_5MicrogramsPerCubicMeter.finiteOrNull()
            ?.takeIf { it >= 0.0 }
        val co2 = snapshot.co2Ppm?.takeIf { it >= 0 }
        val score = AQI.getAQI(pm25 = pm25, co2 = co2).scoreString
        "$score/$AQI_MAXIMUM"
    }

    private fun formatDewPoint(
        humidityPercent: Double?,
        temperatureCelsius: Double?,
        unit: UnitType.TemperatureUnit,
    ): String {
        val humidity = humidityPercent.finiteOrNull()
            ?.takeIf { it > 0.0 && it <= MAX_RELATIVE_HUMIDITY_PERCENT }
            ?: return UNAVAILABLE_VALUE
        val temperature = temperatureCelsius.finiteOrNull()
            ?.takeIf { it in MIN_DEW_POINT_TEMPERATURE_C..MAX_DEW_POINT_TEMPERATURE_C }
            ?: return UNAVAILABLE_VALUE
        val dewPointCelsius = HumidityConverter(
            celsiusTemperature = temperature,
            relativeHumidityPercent = humidity,
        ).toDewCelsius.finiteOrNull() ?: return UNAVAILABLE_VALUE
        val converted = when (unit) {
            UnitType.TemperatureUnit.Celsius -> dewPointCelsius
            UnitType.TemperatureUnit.Fahrenheit ->
                TemperatureConverter.celsiusToFahrenheit(dewPointCelsius)
            UnitType.TemperatureUnit.Kelvin ->
                TemperatureConverter.celsiusToKelvin(dewPointCelsius)
        }
        return unitsConverter.getValueWithoutUnit(
            converted.finiteOrNull(),
            unitsConverter.getHumidityAccuracy(),
        )
    }

    private fun unitResource(type: WidgetType): Int = when (type) {
        WidgetType.DEW_POINT_C -> UnitType.TemperatureUnit.Celsius.unit
        WidgetType.DEW_POINT_F -> UnitType.TemperatureUnit.Fahrenheit.unit
        WidgetType.DEW_POINT_K -> UnitType.TemperatureUnit.Kelvin.unit
        else -> type.unitType.unit
    }

    private fun finiteValueFormatter(
        value: (WidgetSensorSnapshot) -> Double?,
        format: (Double) -> String,
    ): WidgetValueFormatter = { snapshot ->
        value(snapshot).finiteOrNull()?.let(format) ?: UNAVAILABLE_VALUE
    }

    private fun integerFormatter(
        value: (WidgetSensorSnapshot) -> Int?,
        format: (Int) -> String = Int::toString,
    ): WidgetValueFormatter = { snapshot ->
        value(snapshot)?.let(format) ?: UNAVAILABLE_VALUE
    }

    private fun String.canonicalValue(): String =
        trim().ifEmpty { UNAVAILABLE_VALUE }

    private fun Double?.finiteOrNull(): Double? =
        this?.takeIf { it.isFinite() }

    companion object {
        private const val UNAVAILABLE_VALUE = UnitsConverter.NO_VALUE_AVAILABLE
        private const val AQI_MAXIMUM = 100
        private const val MAX_RELATIVE_HUMIDITY_PERCENT = 100.0
        private const val MIN_DEW_POINT_TEMPERATURE_C = -100.0
        private const val MAX_DEW_POINT_TEMPERATURE_C = 370.0
    }
}
