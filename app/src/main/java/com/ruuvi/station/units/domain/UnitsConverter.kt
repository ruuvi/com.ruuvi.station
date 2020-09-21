package com.ruuvi.station.units.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.units.model.TemperatureUnit

class UnitsConverter (
        private val context: Context,
        private val preferences: PreferencesRepository
) {

    // Temperature

    fun getTemperatureUnit(): TemperatureUnit = preferences.getTemperatureUnit()

    fun getAllTemperatureUnits(): Array<TemperatureUnit> = TemperatureUnit.values()

    fun getTemperatureUnitString(): String = context.getString(getTemperatureUnit().unit)

    fun getTemperatureValue(temperatureCelsius: Double): Double {
        return when (getTemperatureUnit()) {
            TemperatureUnit.CELSIUS -> temperatureCelsius
            TemperatureUnit.KELVIN-> TemperatureConverter.celsiusToKelvin(temperatureCelsius)
            TemperatureUnit.FAHRENHEIT -> TemperatureConverter.celsiusToFahrenheit(temperatureCelsius)
        }
    }

    fun getTemperatureString(temperature: Double): String =
            context.getString(R.string.temperature_reading, getTemperatureValue(temperature)) + getTemperatureUnitString()

    fun getTemperatureStringWithoutUnit(temperature: Double): String =
        context.getString(R.string.temperature_reading, getTemperatureValue(temperature))

    // Pressure

    fun getPressureUnit(): PressureUnit = preferences.getPressureUnit()

    fun getAllPressureUnits(): Array<PressureUnit> = PressureUnit.values()

    fun getPressureUnitString(): String = context.getString(getPressureUnit().unit)

    fun getPressureValue(pressurePascal: Double): Double {
        return when (getPressureUnit()) {
            PressureUnit.PA -> pressurePascal
            PressureUnit.HPA-> PressureConverter.pascalToHectopascal(pressurePascal)
            PressureUnit.MMHG -> PressureConverter.pascalToMmMercury(pressurePascal)
            PressureUnit.INHG -> PressureConverter.pascalToInchMercury(pressurePascal)
        }
    }

    fun getPressureString(pressure: Double): String {
        val pressureString = if (getPressureUnit() == PressureUnit.PA) {
            context.getString(R.string.pressure_reading_pa, getPressureValue(pressure), getPressureUnitString())// + getPressureUnitString()
        } else {
            context.getString(R.string.pressure_reading, getPressureValue(pressure), getPressureUnitString())// + getPressureUnitString()
        }

        return if (pressureString.contains("0,00")) {
            pressureString.replace("0,00", "-")
        } else {
            pressureString
        }
    }

    // Humidity

    fun getHumidityUnit(): HumidityUnit = preferences.getHumidityUnit()

    fun getAllHumidityUnits(): Array<HumidityUnit> = HumidityUnit.values()

    fun getHumidityUnitString(): String = context.getString(getHumidityUnit().unit)

    fun getHumidityValue(humidity: Double, temperature: Double): Double {
        val converter = HumidityConverter(temperature, humidity/100)

        return when (getHumidityUnit()) {
            HumidityUnit.PERCENT -> humidity
            HumidityUnit.GM3-> converter.ah
            HumidityUnit.DEW -> {
                when (getTemperatureUnit()) {
                    TemperatureUnit.CELSIUS -> converter.Td
                    TemperatureUnit.KELVIN-> converter.TdK
                    TemperatureUnit.FAHRENHEIT -> converter.TdF
                }
            }
        } ?: 0.0
    }

    fun getHumidityString(humidity: Double, temperature: Double): String {
        val humidityString: String = if (getHumidityUnit() == HumidityUnit.DEW) {
            context.getString(R.string.humidity_reading, getHumidityValue(humidity, temperature)) + getTemperatureUnitString()
        } else {
            context.getString(R.string.humidity_reading, getHumidityValue(humidity, temperature)) + getHumidityUnitString()
        }

        return if (humidityString.contains("0,00")) {
            humidityString.replace("0,00", "-")
        } else {
            humidityString
        }
    }

    fun getSignalString(rssi: Int): String =
        if (rssi != 0) {
            context.getString(R.string.signal_reading, rssi)
        } else {
            context.getString(R.string.signal_reading_zero)
        }
}