package com.ruuvi.station.units.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.units.domain.TemperatureConverter.Companion.fahrenheitMultiplier
import com.ruuvi.station.units.model.*
import com.ruuvi.station.util.extensions.equalsEpsilon
import com.ruuvi.station.util.extensions.isInteger
import com.ruuvi.station.util.extensions.round
class UnitsConverter (
        private val context: Context,
        private val preferences: PreferencesRepository
) {

    // Temperature
    fun getTemperatureEnvironmentValue(
        temperatureCelsius: Double,
        accuracy: Accuracy = getTemperatureAccuracy()
    ): EnvironmentValue =
        EnvironmentValue (
            original = temperatureCelsius,
            value = getTemperatureValue(temperatureCelsius),
            accuracy = accuracy,
            valueWithUnit = getTemperatureString(temperatureCelsius, accuracy),
            valueWithoutUnit = getTemperatureStringWithoutUnit(temperatureCelsius),
            unitString = getTemperatureUnitString()
        )

    fun getTemperatureUnit(): TemperatureUnit = preferences.getTemperatureUnit()

    fun getAllTemperatureUnits(): Array<TemperatureUnit> = TemperatureUnit.values()

    fun getTemperatureUnitString(unit: TemperatureUnit = getTemperatureUnit()): String =
        context.getString(unit.unit)

    fun getTemperatureValue(temperatureCelsius: Double): Double {
        return when (getTemperatureUnit()) {
            TemperatureUnit.CELSIUS -> temperatureCelsius.round(2)
            TemperatureUnit.KELVIN-> TemperatureConverter.celsiusToKelvin(temperatureCelsius)
            TemperatureUnit.FAHRENHEIT -> TemperatureConverter.celsiusToFahrenheit(temperatureCelsius)
        }
    }

    fun getTemperatureCelsiusValue(temperature: Double): Double {
        return when (getTemperatureUnit()) {
            TemperatureUnit.CELSIUS -> temperature
            TemperatureUnit.KELVIN-> TemperatureConverter.kelvinToCelsius(temperature)
            TemperatureUnit.FAHRENHEIT -> TemperatureConverter.fahrenheitToCelsius(temperature)
        }
    }

    fun getTemperatureOffsetValue(temperature: Double): Double {
        return when (getTemperatureUnit()) {
            TemperatureUnit.CELSIUS -> temperature
            TemperatureUnit.KELVIN-> temperature
            TemperatureUnit.FAHRENHEIT -> (temperature * fahrenheitMultiplier).round(2)
        }
    }

    fun getTemperatureString(temperature: Double?, accuracy: Accuracy = getTemperatureAccuracy()): String =
        if (temperature == null) {
            NO_VALUE_AVAILABLE
        } else {
            getTemperatureRawString(getTemperatureValue(temperature), accuracy)
        }

    fun getTemperatureRawString(temperature: Double, accuracy: Accuracy = getTemperatureAccuracy()): String {
        return context.getString(accuracy.nameTemplateId, temperature, getTemperatureUnitString())
    }

    fun getTemperatureRawWithoutUnitString(temperature: Double, accuracy: Accuracy? = getTemperatureAccuracy()): String {
        val temperatureAccuracy = accuracy ?: getTemperatureAccuracy()
        return context.getString(temperatureAccuracy.nameTemplateId, temperature, "").trim()
    }

    fun getTemperatureAccuracy() = preferences.getTemperatureAccuracy()

    fun getTemperatureStringWithoutUnit(temperature: Double?, accuracy: Accuracy = getTemperatureAccuracy()): String =
        if (temperature == null) {
            NO_VALUE_AVAILABLE
        } else {
            context.getString(accuracy.nameTemplateId, getTemperatureValue(temperature), "").trim()
        }

    fun getTemperatureOffsetString(offset: Double): String =
        context.getString(R.string.temperature_reading, getTemperatureOffsetValue(offset), getTemperatureUnitString())

    // Pressure
    fun getPressureEnvironmentValue(
        pressurePascal: Double,
        accuracy: Accuracy = getPressureAccuracy()
    ): EnvironmentValue =
        EnvironmentValue (
            original = pressurePascal,
            value = getPressureValue(pressurePascal),
            accuracy = accuracy,
            valueWithUnit = getPressureString(pressurePascal, accuracy),
            valueWithoutUnit = getPressureStringWithoutUnit(pressurePascal),
            unitString = getPressureUnitString()
        )

    fun getPressureUnit(): PressureUnit = preferences.getPressureUnit()

    fun getAllPressureUnits(): Array<PressureUnit> = PressureUnit.values()

    fun getPressureUnitString(pressureUnit: PressureUnit = getPressureUnit()): String {
        return context.getString(pressureUnit.unit)
    }

    fun getPressureValue(pressurePascal: Double): Double {
        return when (getPressureUnit()) {
            PressureUnit.PA -> pressurePascal
            PressureUnit.HPA-> PressureConverter.pascalToHectopascal(pressurePascal)
            PressureUnit.MMHG -> PressureConverter.pascalToMmMercury(pressurePascal)
            PressureUnit.INHG -> PressureConverter.pascalToInchMercury(pressurePascal)
        }
    }

    fun getPressurePascalValue(pressure: Double): Double {
        return when (getPressureUnit()) {
            PressureUnit.PA -> pressure
            PressureUnit.HPA-> PressureConverter.hectopascalToPascal(pressure)
            PressureUnit.INHG -> PressureConverter.inchMercuryToPascal(pressure)
            PressureUnit.MMHG -> PressureConverter.mmMercuryToPascal(pressure)
        }
    }

    fun getPressureString(pressure: Double?, accuracy: Accuracy? = null): String {
        return if (pressure == null) {
            NO_VALUE_AVAILABLE
        } else {
            getPressureRawString(getPressureValue(pressure), accuracy)
        }
    }

    fun getPressureRawString(pressure: Double, accuracy: Accuracy? = null): String {
        return if (getPressureUnit() == PressureUnit.PA) {
            context.getString(R.string.pressure_reading_pa, pressure, getPressureUnitString())
        } else {
            val displayAccuracy = accuracy ?: getPressureAccuracy()
            return context.getString(displayAccuracy.nameTemplateId, pressure, getPressureUnitString())
        }
    }

    fun getPressureRawWithoutUnitString(pressure: Double, accuracy: Accuracy? = getPressureAccuracy()): String {
        val pressureAccuracy = accuracy ?: getPressureAccuracy()
        return if (getPressureUnit() == PressureUnit.PA) {
            context.getString(R.string.pressure_reading_pa, pressure, "").trim()
        } else {
            return context.getString(pressureAccuracy.nameTemplateId, pressure, "").trim()
        }
    }

    fun getPressureStringWithoutUnit(pressure: Double?, accuracy: Accuracy = getPressureAccuracy()): String =
        if (pressure == null) {
            NO_VALUE_AVAILABLE
        } else {
            if (getPressureUnit() == PressureUnit.PA) {
                context.getString(R.string.pressure_reading_pa, getPressureValue(pressure), "").trim()
            } else {
                context.getString(accuracy.nameTemplateId, getPressureValue(pressure), "").trim()
            }
        }

    fun getPressureAccuracy() = preferences.getPressureAccuracy()

    // Humidity
    fun getHumidityEnvironmentValue(
        humidity: Double,
        temperature: Double?,
        accuracy: Accuracy = getHumidityAccuracy()
    ): EnvironmentValue? =
        getHumidityValue(humidity, temperature)?.let {
            EnvironmentValue (
                original = humidity,
                value = it,
                accuracy = accuracy,
                valueWithUnit = getHumidityString(humidity, temperature),
                valueWithoutUnit = getHumidityStringWithoutUnit(humidity, temperature),
                unitString = getHumidityUnitString()
            )
        }


    fun getHumidityUnit(): HumidityUnit = preferences.getHumidityUnit()

    fun getAllHumidityUnits(): Array<HumidityUnit> = HumidityUnit.values()

    fun getHumidityUnitString(humidityUnit: HumidityUnit = getHumidityUnit()): String {
        return if (humidityUnit != HumidityUnit.DEW) {
            context.getString(humidityUnit.unit)
        } else {
            getTemperatureUnitString()
        }
    }

    fun getHumidityValue(humidity: Double, temperature: Double?, humidityUnit: HumidityUnit = getHumidityUnit()): Double? {
        val converter = temperature?.let { HumidityConverter(temperature, humidity/100) }

        return when (humidityUnit) {
            HumidityUnit.PERCENT -> humidity.round(2)
            HumidityUnit.GM3-> converter?.let { it.absoluteHumidity.round(2) }
            HumidityUnit.DEW -> {
                converter?.let {
                    when (getTemperatureUnit()) {
                        TemperatureUnit.CELSIUS -> (converter.toDewCelsius ?: 0.0).round(2)
                        TemperatureUnit.KELVIN -> (converter.toDewKelvin ?: 0.0).round(2)
                        TemperatureUnit.FAHRENHEIT -> (converter.toDewFahrenheit ?: 0.0).round(2)
                    }
                }
            }
        }
    }

    fun getHumidityStringWithoutUnit(humidity: Double?, temperature: Double?): String =
        if (humidity == null) {
            NO_VALUE_AVAILABLE
        } else {
            val humidityValue = getHumidityValue(humidity, temperature)
            if (humidityValue == null) {
                NO_VALUE_AVAILABLE
            } else {
                context.getString(getHumidityAccuracy().nameTemplateId, humidityValue, "").trim()
            }

        }

    fun getHumidityString(
        humidity: Double?,
        temperature: Double?,
        humidityUnit: HumidityUnit = getHumidityUnit(),
        accuracy: Accuracy? = null
    ): String {
        return if (humidity == null || temperature == null) {
            NO_VALUE_AVAILABLE
        } else {
            getHumidityRawString(getHumidityValue(humidity, temperature, humidityUnit), accuracy, humidityUnit)
        }
    }

    fun getHumidityRawString(
        humidity: Double?,
        accuracy: Accuracy? = null,
        humidityUnit: HumidityUnit? = getHumidityUnit()
    ): String {
        if (humidity == null)
            return NO_VALUE_AVAILABLE
        val displayAccuracy = accuracy ?: getHumidityAccuracy()
        val humidityUnitString = humidityUnit?.let { getHumidityUnitString(humidityUnit) } ?: ""
        return context.getString(displayAccuracy.nameTemplateId, humidity, humidityUnitString).trim()
    }

    fun getHumidityRawWithoutUnitString(
        hunidity: Double,
        accuracy: Accuracy? = getHumidityAccuracy()
    ): String {
        val humidityAccuracy = accuracy ?: getHumidityAccuracy()
        return context.getString(humidityAccuracy.nameTemplateId, hunidity, "").trim()
    }

    fun getHumidityAccuracy() = preferences.getHumidityAccuracy()

    fun getSignalString(rssi: Int): String =
        if (rssi != 0) {
            context.getString(R.string.signal_reading, rssi, getSignalUnit())
        } else {
            context.getString(R.string.signal_reading_zero, getSignalUnit())
        }

    fun getSignalUnit(): String {
        return context.getString(R.string.signal_unit)
    }

    fun getDisplayValue(value: Float): String {
        if (value.isInteger(0.009f)) {
            return getDisplayApproximateValue(value)
        } else {
            return getDisplayPreciseValue(value)
        }
    }
    fun getDisplayPreciseValue(value: Float): String {
        if (value.equalsEpsilon(value.round(1), 0.0001f)) {
            return String.format("%1$,.1f", value)
        } else {
            return String.format("%1$,.2f", value)
        }
    }

    fun getDisplayApproximateValue(value: Float): String {
        return value.round(0).toInt().toString()
    }

    fun getVoltageEnvironmentValue(
        voltage: Double
    ): EnvironmentValue =
        EnvironmentValue (
            original = voltage,
            value = voltage,
            accuracy = Accuracy.Accuracy2,
            valueWithUnit = context.getString(R.string.voltage_reading, voltage, context.getString(R.string.voltage_unit)),
            valueWithoutUnit = context.getString(R.string.voltage_reading, voltage, "").trim(),
            unitString = context.getString(R.string.voltage_unit)
        )


    fun getSignalEnvironmentValue(
        rssi: Int
    ): EnvironmentValue =
        EnvironmentValue (
            original = rssi.toDouble(),
            value = rssi.toDouble(),
            accuracy = Accuracy.Accuracy0,
            valueWithUnit = getSignalString(rssi),
            valueWithoutUnit = context.getString(R.string.signal_reading, rssi, "").trim(),
            unitString = getSignalUnit()
        )

    fun getPmEnvironmentValue(pm: Double): EnvironmentValue =
        EnvironmentValue(
            original = pm,
            value = pm,
            accuracy = Accuracy.Accuracy1,
            valueWithUnit = context.getString(Accuracy.Accuracy1.nameTemplateId, pm, "µg/m³"),
            valueWithoutUnit = context.getString(Accuracy.Accuracy1.nameTemplateId, pm, "").trim(),
            unitString = "µg/m³"
        )

    fun getCo2EnvironmentValue(co2: Int) =
        EnvironmentValue(
            original = co2.toDouble(),
            value = co2.toDouble(),
            accuracy = Accuracy.Accuracy1,
            valueWithUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, co2.toDouble(), "ppm"),
            valueWithoutUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, co2.toDouble(), "").trim(),
            unitString = "ppm"
        )

    fun getVocEnvironmentValue(voc: Int) =
        EnvironmentValue(
            original = voc.toDouble(),
            value = voc.toDouble(),
            accuracy = Accuracy.Accuracy1,
            valueWithUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, voc.toDouble(), "TVOC"),
            valueWithoutUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, voc.toDouble(), "").trim(),
            unitString = "TVOC"
        )

    fun getNoxEnvironmentValue(nox: Int) =
        EnvironmentValue(
            original = nox.toDouble(),
            value = nox.toDouble(),
            accuracy = Accuracy.Accuracy1,
            valueWithUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, nox.toDouble(), "NOX"),
            valueWithoutUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, nox.toDouble(), "").trim(),
            unitString = "NOX"
        )

    fun getLuminosityEnvironmentValue(luminosity: Int) =
        EnvironmentValue(
            original = luminosity.toDouble(),
            value = luminosity.toDouble(),
            accuracy = Accuracy.Accuracy1,
            valueWithUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, luminosity.toDouble(), "lux"),
            valueWithoutUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, luminosity.toDouble(), "").trim(),
            unitString = "lux"
        )

    fun getNoiseEnvironmentValue(dba: Double) =
        EnvironmentValue(
            original = dba,
            value = dba,
            accuracy = Accuracy.Accuracy1,
            valueWithUnit = context.getString(Accuracy.Accuracy1.nameTemplateId, dba, "dB"),
            valueWithoutUnit = context.getString(Accuracy.Accuracy1.nameTemplateId, dba, "").trim(),
            unitString = "dB"
        )

    companion object {
        const val NO_VALUE_AVAILABLE = "-"
    }
}