package com.ruuvi.station.widgets.data

import com.ruuvi.station.R
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.model.UnitType

enum class WidgetType(val code: Int, val titleResId: Int, val unitType: UnitType) {
    TEMPERATURE(1, R.string.temperature, UnitType.TemperatureUnit.Celsius),
    HUMIDITY(2, R.string.humidity, UnitType.HumidityUnit.Relative),
    PRESSURE(3, R.string.pressure, UnitType.PressureUnit.HectoPascal),
    MOVEMENT(4, R.string.movement_counter, UnitType.MovementUnit.MovementsCount),
    VOLTAGE(5, R.string.battery_voltage, UnitType.BatteryVoltageUnit.Volt),
    SIGNAL_STRENGTH(6, R.string.signal_strength_rssi, UnitType.SignalStrengthUnit.SignalDbm),
    ACCELERATION_X(7, R.string.acceleration_x, UnitType.Acceleration.GForceX),
    ACCELERATION_Y(8, R.string.acceleration_y, UnitType.Acceleration.GForceY),
    ACCELERATION_Z(9, R.string.acceleration_z, UnitType.Acceleration.GForceZ),
    AIR_QUALITY(21, R.string.air_quality, UnitType.AirQuality.AqiIndex),
    LUMINOSITY(22, R.string.luminosity, UnitType.Luminosity.Lux),
    CO2(23, R.string.co2, UnitType.CO2.Ppm),
    VOC(23, R.string.voc, UnitType.VOC.VocIndex),
    NOX(24, R.string.nox, UnitType.NOX.NoxIndex),
    PM10(25, R.string.pm1, UnitType.PM1.Mgm3),
    PM25(26, R.string.pm25, UnitType.PM25.Mgm3),
    PM40(27, R.string.pm4, UnitType.PM4.Mgm3),
    PM100(28, R.string.pm10, UnitType.PM10.Mgm3);


    companion object {
        fun getByCode(code: Int): WidgetType = values().firstOrNull{it.code == code} ?: TEMPERATURE

        fun filterWidgetTypes(sensor: RuuviTag): List<WidgetType> {
            val result = mutableListOf<WidgetType>()
            for (item in WidgetType.entries) {
                if (sensor.possibleDisplayOptions.any{ it == item.unitType} || sensor.displayOrder.any{ it == item.unitType}) {
                    result.add(item)
                }
            }
            return result
        }
    }
}