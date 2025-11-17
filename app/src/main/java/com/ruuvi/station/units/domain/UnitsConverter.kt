package com.ruuvi.station.units.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.units.domain.TemperatureConverter.Companion.fahrenheitMultiplier
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.*
import com.ruuvi.station.units.model.UnitType.*
import com.ruuvi.station.util.extensions.equalsEpsilon
import com.ruuvi.station.util.extensions.isInteger
import com.ruuvi.station.util.extensions.round
class UnitsConverter (
        private val context: Context,
        private val preferences: PreferencesRepository
) {
    fun getTitleForUnitType(unitType: UnitType): String {
        val unit = if (unitType is UnitType.HumidityUnit.DewPoint) {
            context.getString(getTemperatureUnit().unit)
        } else {
            context.getString(unitType.unit)
        }

        val unitString = if (unit.isNotEmpty()) " ($unit)" else ""
        val name = context.getString(unitType.measurementName)
        return name + unitString
    }

    // AQI
    fun getAqiEnviromentValue(
        value: AQI
    ): EnvironmentValue {
        if (value.score != null) {
            val aqi = getValueWithoutUnit(value.score.toDouble(), Accuracy.Accuracy0)
            return EnvironmentValue(
                original = value.score.toDouble(),
                value = value.score.toDouble(),
                accuracy = Accuracy.Accuracy0,
                valueWithUnit = "$aqi/100 ${context.getString(AirQuality.AqiIndex.unit)}",
                valueWithoutUnit = "$aqi/100",
                unitString = context.getString(AirQuality.AqiIndex.unit),
                unitType = AirQuality.AqiIndex
            )
        } else {
            return EnvironmentValue(
                original = 0.toDouble(),
                value = 0.toDouble(),
                accuracy = Accuracy.Accuracy0,
                valueWithUnit = "$NO_VALUE_AVAILABLE/100 ${context.getString(R.string.air_quality)}",
                valueWithoutUnit = "$NO_VALUE_AVAILABLE/100",
                unitString = context.getString(R.string.air_quality),
                unitType = AirQuality.AqiIndex
            )
        }
    }

    // Temperature
    fun getTemperatureEnvironmentValue(
        temperatureCelsius: Double,
        temperatureUnit: TemperatureUnit = getTemperatureUnit(),
        accuracy: Accuracy = getTemperatureAccuracy()
    ): EnvironmentValue =
        EnvironmentValue (
            original = temperatureCelsius,
            value = getTemperatureValue(temperatureCelsius, temperatureUnit),
            accuracy = accuracy,
            valueWithUnit = getTemperatureString(temperatureCelsius, temperatureUnit, accuracy),
            valueWithoutUnit = getTemperatureStringWithoutUnit(temperatureCelsius, temperatureUnit),
            unitString = getTemperatureUnitString(temperatureUnit),
            unitType = temperatureUnit
        )

    fun getTemperatureUnit(): TemperatureUnit = preferences.getTemperatureUnit()

    fun getAllTemperatureUnits(): List<TemperatureUnit> = TemperatureUnit.getUnits()

    fun getTemperatureUnitString(unit: TemperatureUnit = getTemperatureUnit()): String =
        context.getString(unit.unit)

    fun getTemperatureValue(
        temperatureCelsius: Double,
        temperatureUnit: TemperatureUnit = getTemperatureUnit()
    ): Double {
        return when (temperatureUnit) {
            TemperatureUnit.Celsius -> temperatureCelsius.round(2)
            TemperatureUnit.Kelvin-> TemperatureConverter.celsiusToKelvin(temperatureCelsius)
            TemperatureUnit.Fahrenheit -> TemperatureConverter.celsiusToFahrenheit(temperatureCelsius)
        }
    }

    fun getTemperatureCelsiusValue(temperature: Double): Double {
        return when (getTemperatureUnit()) {
            TemperatureUnit.Celsius -> temperature
            TemperatureUnit.Kelvin-> TemperatureConverter.kelvinToCelsius(temperature)
            TemperatureUnit.Fahrenheit -> TemperatureConverter.fahrenheitToCelsius(temperature)
        }
    }

    fun getTemperatureOffsetValue(temperature: Double): Double {
        return when (getTemperatureUnit()) {
            TemperatureUnit.Celsius -> temperature
            TemperatureUnit.Kelvin-> temperature
            TemperatureUnit.Fahrenheit -> (temperature * fahrenheitMultiplier).round(2)
        }
    }

    fun getTemperatureString(
        temperature: Double?,
        temperatureUnit: TemperatureUnit = getTemperatureUnit(),
        accuracy: Accuracy = getTemperatureAccuracy()
    ): String =
        if (temperature == null) {
            NO_VALUE_AVAILABLE
        } else {
            getTemperatureRawString(getTemperatureValue(temperature, temperatureUnit), accuracy)
        }

    fun getTemperatureRawString(temperature: Double, accuracy: Accuracy = getTemperatureAccuracy()): String {
        return context.getString(accuracy.nameTemplateId, temperature, getTemperatureUnitString())
    }

    fun getTemperatureRawWithoutUnitString(temperature: Double, accuracy: Accuracy? = getTemperatureAccuracy()): String {
        val temperatureAccuracy = accuracy ?: getTemperatureAccuracy()
        return context.getString(temperatureAccuracy.nameTemplateId, temperature, "").trim()
    }

    fun getTemperatureAccuracy() = preferences.getTemperatureAccuracy()

    fun getTemperatureStringWithoutUnit(
        temperature: Double?,
        temperatureUnit: TemperatureUnit = getTemperatureUnit(),
        accuracy: Accuracy = getTemperatureAccuracy()
    ): String =
        if (temperature == null) {
            NO_VALUE_AVAILABLE
        } else {
            context.getString(accuracy.nameTemplateId, getTemperatureValue(temperature, temperatureUnit), "").trim()
        }

    fun getTemperatureOffsetString(offset: Double): String =
        context.getString(R.string.temperature_reading, getTemperatureOffsetValue(offset), getTemperatureUnitString())

    // Pressure
    fun getPressureEnvironmentValue(
        pressurePascal: Double,
        pressureUnit: PressureUnit = getPressureUnit(),
        accuracy: Accuracy = getPressureAccuracy()
    ): EnvironmentValue =
        EnvironmentValue (
            original = pressurePascal,
            value = getPressureValue(pressurePascal, pressureUnit),
            accuracy = accuracy,
            valueWithUnit = getPressureString(pressurePascal, pressureUnit, accuracy),
            valueWithoutUnit = getPressureStringWithoutUnit(pressurePascal, pressureUnit),
            unitString = getPressureUnitString(pressureUnit),
            unitType = pressureUnit
        )

    fun getPressureUnit(): PressureUnit = preferences.getPressureUnit()

    fun getAllPressureUnits(): List<PressureUnit> = PressureUnit.getUnits()

    fun getPressureUnitString(pressureUnit: PressureUnit = getPressureUnit()): String {
        return context.getString(pressureUnit.unit)
    }

    fun getPressureValue(
        pressurePascal: Double,
        pressureUnit: PressureUnit = getPressureUnit()
    ): Double {
        return when (pressureUnit) {
            PressureUnit.Pascal -> pressurePascal
            PressureUnit.HectoPascal-> PressureConverter.pascalToHectopascal(pressurePascal)
            PressureUnit.MmHg -> PressureConverter.pascalToMmMercury(pressurePascal)
            PressureUnit.InchHg -> PressureConverter.pascalToInchMercury(pressurePascal)
        }
    }

    fun getPressurePascalValue(pressure: Double): Double {
        return when (getPressureUnit()) {
            PressureUnit.Pascal -> pressure
            PressureUnit.HectoPascal-> PressureConverter.hectopascalToPascal(pressure)
            PressureUnit.InchHg -> PressureConverter.inchMercuryToPascal(pressure)
            PressureUnit.MmHg -> PressureConverter.mmMercuryToPascal(pressure)
        }
    }

    fun getPressureString(
        pressure: Double?,
        pressureUnit: PressureUnit = getPressureUnit(),
        accuracy: Accuracy? = null
    ): String {
        return if (pressure == null) {
            NO_VALUE_AVAILABLE
        } else {
            getPressureRawString(getPressureValue(pressure, pressureUnit), pressureUnit, accuracy)
        }
    }

    fun getPressureRawString(
        pressure: Double,
        pressureUnit: PressureUnit = getPressureUnit(),
        accuracy: Accuracy? = null
    ): String {
        return if (pressureUnit == PressureUnit.Pascal) {
            context.getString(R.string.pressure_reading_pa, pressure, getPressureUnitString(pressureUnit))
        } else {
            val displayAccuracy = accuracy ?: getPressureAccuracy()
            return context.getString(displayAccuracy.nameTemplateId, pressure, getPressureUnitString(pressureUnit))
        }
    }

    fun getPressureRawWithoutUnitString(pressure: Double, accuracy: Accuracy? = getPressureAccuracy()): String {
        val pressureAccuracy = accuracy ?: getPressureAccuracy()
        return if (getPressureUnit() == PressureUnit.Pascal) {
            context.getString(R.string.pressure_reading_pa, pressure, "").trim()
        } else {
            return context.getString(pressureAccuracy.nameTemplateId, pressure, "").trim()
        }
    }

    fun getPressureStringWithoutUnit(
        pressure: Double?,
        pressureUnit: PressureUnit = getPressureUnit(),
        accuracy: Accuracy = getPressureAccuracy()
    ): String =
        if (pressure == null) {
            NO_VALUE_AVAILABLE
        } else {
            if (pressureUnit == PressureUnit.Pascal) {
                context.getString(R.string.pressure_reading_pa, getPressureValue(pressure, pressureUnit), "").trim()
            } else {
                context.getString(accuracy.nameTemplateId, getPressureValue(pressure, pressureUnit), "").trim()
            }
        }

    fun getPressureAccuracy() = preferences.getPressureAccuracy()

    // Humidity
    fun getHumidityEnvironmentValue(
        humidity: Double,
        temperature: Double?,
        humidityUnit: HumidityUnit = getHumidityUnit(),
        accuracy: Accuracy = getHumidityAccuracy()
    ): EnvironmentValue? =
        getHumidityValue(humidity, temperature, humidityUnit)?.let {
            EnvironmentValue (
                original = humidity,
                value = it,
                accuracy = accuracy,
                valueWithUnit = getHumidityString(humidity, temperature, humidityUnit),
                valueWithoutUnit = getHumidityStringWithoutUnit(humidity, temperature, humidityUnit),
                unitString = getHumidityUnitString(humidityUnit),
                unitType = humidityUnit
            )
        }


    fun getHumidityUnit(): HumidityUnit = preferences.getHumidityUnit()

    fun getAllHumidityUnits(): List<HumidityUnit> = HumidityUnit.getUnits()

    fun getHumidityUnitString(humidityUnit: HumidityUnit = getHumidityUnit()): String {
        return if (humidityUnit != HumidityUnit.DewPoint) {
            context.getString(humidityUnit.unit)
        } else {
            getTemperatureUnitString()
        }
    }

    fun getHumidityValue(
        humidity: Double,
        temperature: Double?,
        humidityUnit: HumidityUnit = getHumidityUnit()
    ): Double? {
        val converter = temperature?.let { HumidityConverter(temperature, humidity/100) }

        return when (humidityUnit) {
            HumidityUnit.Relative -> humidity.round(2)
            HumidityUnit.Absolute-> converter?.let { it.absoluteHumidity.round(2) }
            HumidityUnit.DewPoint -> {
                converter?.let {
                    when (getTemperatureUnit()) {
                        TemperatureUnit.Celsius -> (converter.toDewCelsius ?: 0.0).round(2)
                        TemperatureUnit.Kelvin -> (converter.toDewKelvin ?: 0.0).round(2)
                        TemperatureUnit.Fahrenheit -> (converter.toDewFahrenheit ?: 0.0).round(2)
                    }
                }
            }
        }
    }

    fun getHumidityStringWithoutUnit(
        humidity: Double?,
        temperature: Double?,
        humidityUnit: HumidityUnit = getHumidityUnit()
    ): String =
        if (humidity == null) {
            NO_VALUE_AVAILABLE
        } else {
            val humidityValue = getHumidityValue(humidity, temperature, humidityUnit)
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

    fun getValue(
        value: Double?,
        accuracy: Accuracy,
        unit: String): String
    {
        if (value == null)
            return NO_VALUE_AVAILABLE
        return context.getString(accuracy.nameTemplateId, value, unit).trim()
    }

    fun getValueWithoutUnit(
        value: Double?,
        accuracy: Accuracy, ): String
    {
        if (value == null)
            return NO_VALUE_AVAILABLE
        return context.getString(accuracy.nameTemplateId, value, "").trim()
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
            unitString = context.getString(R.string.voltage_unit),
            unitType = BatteryVoltageUnit.Volt
        )

    fun getAccelerationValue(
        value: Double,
        unit: Acceleration
    ): EnvironmentValue =
        EnvironmentValue (
            original = value,
            value = value,
            accuracy = Accuracy.Accuracy2,
            valueWithUnit = context.getString(R.string.acceleration_reading, value, context.getString(unit.unit)),
            valueWithoutUnit = context.getString(R.string.acceleration_reading, value, "").trim(),
            unitString = context.getString(unit.unit),
            unitType = unit
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
            unitString = getSignalUnit(),
            unitType = SignalStrengthUnit.SignalDbm
        )

    fun getPmEnvironmentValue(pm: Double, unitType: UnitType): EnvironmentValue =
        EnvironmentValue(
            original = pm,
            value = pm,
            accuracy = Accuracy.Accuracy1,
            valueWithUnit = context.getString(Accuracy.Accuracy1.nameTemplateId, pm, context.getString(unitType.unit)),
            valueWithoutUnit = context.getString(Accuracy.Accuracy1.nameTemplateId, pm, "").trim(),
            unitString = context.getString(unitType.unit),
            unitType = unitType
        )

    fun getCo2EnvironmentValue(co2: Int) =
        EnvironmentValue(
            original = co2.toDouble(),
            value = co2.toDouble(),
            accuracy = Accuracy.Accuracy1,
            valueWithUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, co2.toDouble(), context.getString(CO2.Ppm.unit)),
            valueWithoutUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, co2.toDouble(), "").trim(),
            unitString = context.getString(CO2.Ppm.unit),
            unitType = CO2.Ppm
        )

    fun getVocEnvironmentValue(voc: Int) =
        EnvironmentValue(
            original = voc.toDouble(),
            value = voc.toDouble(),
            accuracy = Accuracy.Accuracy1,
            valueWithUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, voc.toDouble(), context.getString(VOC.VocIndex.unit)),
            valueWithoutUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, voc.toDouble(), "").trim(),
            unitString = context.getString(VOC.VocIndex.unit),
            unitType = VOC.VocIndex
        )

    fun getNoxEnvironmentValue(nox: Int) =
        EnvironmentValue(
            original = nox.toDouble(),
            value = nox.toDouble(),
            accuracy = Accuracy.Accuracy1,
            valueWithUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, nox.toDouble(), context.getString(NOX.NoxIndex.unit)),
            valueWithoutUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, nox.toDouble(), "").trim(),
            unitString = context.getString(NOX.NoxIndex.unit),
            unitType = NOX.NoxIndex
        )

    fun getLuminosityEnvironmentValue(luminosity: Double) =
        EnvironmentValue(
            original = luminosity,
            value = luminosity,
            accuracy = Accuracy.Accuracy1,
            valueWithUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, luminosity, context.getString(Luminosity.Lux.unit)),
            valueWithoutUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, luminosity, "").trim(),
            unitString = context.getString(Luminosity.Lux.unit),
            unitType = Luminosity.Lux
        )

    fun getSoundEnvironmentValue(dba: Double, unitType: UnitType) =
        EnvironmentValue(
            original = dba,
            value = dba,
            accuracy = Accuracy.Accuracy1,
            valueWithUnit = context.getString(Accuracy.Accuracy1.nameTemplateId, dba, context.getString(unitType.unit)),
            valueWithoutUnit = context.getString(Accuracy.Accuracy1.nameTemplateId, dba, "").trim(),
            unitString = context.getString(unitType.unit),
            unitType = unitType
        )

    fun getMsnValue(
        value: Int
    ): EnvironmentValue =
        EnvironmentValue (
            original = value.toDouble(),
            value = value.toDouble(),
            accuracy = Accuracy.Accuracy0,
            valueWithUnit =context.getString(Accuracy.Accuracy0.nameTemplateId, value.toDouble(), "").trim(),
            valueWithoutUnit = context.getString(Accuracy.Accuracy0.nameTemplateId, value.toDouble(), "").trim(),
            unitString = "",
            unitType = MsnUnit.MsnCount
        )

    companion object {
        const val NO_VALUE_AVAILABLE = "-"
    }
}