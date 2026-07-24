package com.ruuvi.station.widgets.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.model.UnitType

enum class WidgetType(val code: Int, val titleResId: Int, val unitType: UnitType) {
    TEMPERATURE(1, R.string.temperature, UnitType.TemperatureUnit.Celsius),
    HUMIDITY(2, R.string.humidity, UnitType.HumidityUnit.Relative),
    PRESSURE(3, R.string.air_pressure, UnitType.PressureUnit.HectoPascal),
    MOVEMENT(4, R.string.movement_counter, UnitType.MovementUnit.MovementsCount),
    VOLTAGE(5, R.string.battery_voltage, UnitType.BatteryVoltageUnit.Volt),
    SIGNAL_STRENGTH(6, R.string.signal_strength_rssi, UnitType.SignalStrengthUnit.SignalDbm),
    ACCELERATION_X(7, R.string.acceleration_x, UnitType.Acceleration.GForceX),
    ACCELERATION_Y(8, R.string.acceleration_y, UnitType.Acceleration.GForceY),
    ACCELERATION_Z(9, R.string.acceleration_z, UnitType.Acceleration.GForceZ),
    SOUND_AVERAGE(10, R.string.sound_avg, UnitType.SoundAvg.SoundDba),
    SOUND_PEAK(11, R.string.sound_peak, UnitType.SoundPeak.SoundDba),
    MEASUREMENT_SEQUENCE_NUMBER(12, R.string.meas_seq_number, UnitType.MsnUnit.MsnCount),
    TEMPERATURE_F(13, R.string.temperature, UnitType.TemperatureUnit.Fahrenheit),
    TEMPERATURE_K(14, R.string.temperature, UnitType.TemperatureUnit.Kelvin),
    HUMIDITY_ABSOLUTE(15, R.string.abs_humidity, UnitType.HumidityUnit.Absolute),
    DEW_POINT_C(16, R.string.dewpoint, UnitType.HumidityUnit.DewPoint),
    DEW_POINT_F(17, R.string.dewpoint, UnitType.HumidityUnit.DewPoint),
    DEW_POINT_K(18, R.string.dewpoint, UnitType.HumidityUnit.DewPoint),
    PRESSURE_PA(19, R.string.air_pressure, UnitType.PressureUnit.Pascal),
    PRESSURE_MMHG(20, R.string.air_pressure, UnitType.PressureUnit.MmHg),
    AIR_QUALITY(21, R.string.air_quality, UnitType.AirQuality.AqiIndex),
    LUMINOSITY(22, R.string.luminosity, UnitType.Luminosity.Lux),
    CO2(23, R.string.co2, UnitType.CO2.Ppm),
    NOX(24, R.string.nox_index, UnitType.NOX.NoxIndex),
    PM10(25, R.string.pm10, UnitType.PM.PM10),
    PM25(26, R.string.pm25, UnitType.PM.PM25),
    PM40(27, R.string.pm40, UnitType.PM.PM40),
    PM100(28, R.string.pm100, UnitType.PM.PM100),
    VOC(29, R.string.voc_index, UnitType.VOC.VocIndex),
    PRESSURE_INHG(30, R.string.air_pressure, UnitType.PressureUnit.InchHg);

    fun getTitle(context: Context): String {
        val title = context.getString(titleResId)
        val unit = context.getString(unitType.unit)
        return if (unit.isNotEmpty()) "$title ($unit)" else title
    }

    @Composable
    fun title(): String {
        val title = stringResource(titleResId)
        val unit = stringResource(unitType.unit)
        return if (unit.isNotEmpty()) "$title ($unit)" else title
    }

    companion object {
        fun getByCode(code: Int): WidgetType =
            if (code == LEGACY_SOUND_REAL_TIME_CODE) {
                SOUND_AVERAGE
            } else {
                entries.firstOrNull { it.code == code } ?: TEMPERATURE
            }

        fun filterWidgetTypes(sensor: RuuviTag): List<WidgetType> {
            val supportedUnits = (sensor.displayOrder + sensor.possibleDisplayOptions).distinct()
            return displayOrder.filter { widgetType ->
                when (widgetType) {
                    TEMPERATURE, TEMPERATURE_F, TEMPERATURE_K ->
                        supportedUnits.any { it is UnitType.TemperatureUnit }
                    HUMIDITY, HUMIDITY_ABSOLUTE ->
                        supportedUnits.any { it is UnitType.HumidityUnit.Relative || it is UnitType.HumidityUnit.Absolute }
                    DEW_POINT_C, DEW_POINT_F, DEW_POINT_K ->
                        supportedUnits.any { it is UnitType.HumidityUnit.DewPoint }
                    PRESSURE, PRESSURE_PA, PRESSURE_MMHG, PRESSURE_INHG ->
                        supportedUnits.any { it is UnitType.PressureUnit }
                    SOUND_AVERAGE ->
                        supportedUnits.any { it is UnitType.SoundAvg }
                    else -> supportedUnits.any { it == widgetType.unitType }
                }
            }
        }

        fun getTitle(context: Context, widgetType: WidgetType, title: String): String {
            fun u(unitResId: Int) = context.getString(unitResId)
            return when (widgetType) {
                TEMPERATURE ->
                    title + " (" + u(UnitType.TemperatureUnit.Celsius.unit) + ")"
                TEMPERATURE_F ->
                    title + " (" + u(UnitType.TemperatureUnit.Fahrenheit.unit) + ")"
                TEMPERATURE_K ->
                    title + " (" + u(UnitType.TemperatureUnit.Kelvin.unit) + ")"
                HUMIDITY ->
                    title + " (" + u(UnitType.HumidityUnit.Relative.unit) + ")"
                HUMIDITY_ABSOLUTE ->
                    title + " (" + u(UnitType.HumidityUnit.Absolute.unit) + ")"
                DEW_POINT_C ->
                    title + " (" + u(UnitType.TemperatureUnit.Celsius.unit) + ")"
                DEW_POINT_F ->
                    title + " (" + u(UnitType.TemperatureUnit.Fahrenheit.unit) + ")"
                DEW_POINT_K ->
                    title + " (" + u(UnitType.TemperatureUnit.Kelvin.unit) + ")"
                PRESSURE ->
                    title + " (" + u(UnitType.PressureUnit.HectoPascal.unit) + ")"
                PRESSURE_PA ->
                    title + " (" + u(UnitType.PressureUnit.Pascal.unit) + ")"
                PRESSURE_MMHG ->
                    title + " (" + u(UnitType.PressureUnit.MmHg.unit) + ")"
                PRESSURE_INHG ->
                    title + " (" + u(UnitType.PressureUnit.InchHg.unit) + ")"
                else -> title
            }
        }

        internal val displayOrder = listOf(
            AIR_QUALITY,
            CO2,
            PM10,
            PM25,
            PM40,
            PM100,
            VOC,
            NOX,
            TEMPERATURE,
            TEMPERATURE_F,
            TEMPERATURE_K,
            HUMIDITY,
            HUMIDITY_ABSOLUTE,
            DEW_POINT_C,
            DEW_POINT_F,
            DEW_POINT_K,
            PRESSURE,
            PRESSURE_PA,
            PRESSURE_MMHG,
            PRESSURE_INHG,
            MOVEMENT,
            SOUND_AVERAGE,
            SOUND_PEAK,
            LUMINOSITY,
            VOLTAGE,
            ACCELERATION_X,
            ACCELERATION_Y,
            ACCELERATION_Z,
            SIGNAL_STRENGTH,
            MEASUREMENT_SEQUENCE_NUMBER,
        )

        internal const val LEGACY_SOUND_REAL_TIME_CODE = 31
    }
}
