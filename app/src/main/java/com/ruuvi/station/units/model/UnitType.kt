package com.ruuvi.station.units.model

import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmType

sealed class UnitType(
    val unitCode: String,
    val unitTitle: Int,
    val unit: Int,
    val measurementCode: String,
    val measurementTitle: Int,
    val measurementName: Int,
    val iconRes: Int,
    val alarmType: AlarmType? = null,
    val defaultAccuracy: Accuracy,
){

    fun getCode(): String = "${measurementCode}_$unitCode"

    sealed class TemperatureUnit(
        code: String,
        unitTitle: Int,
        unit: Int
    ): UnitType(
        unitCode = code,
        unitTitle = unitTitle,
        unit = unit,
        measurementCode = TEMPERATURE_MEASUREMENT_CODE,
        measurementTitle = R.string.temperature,
        measurementName = R.string.temperature,
        iconRes = R.drawable.icon_temperature,
        alarmType = AlarmType.TEMPERATURE,
        defaultAccuracy = Accuracy.Accuracy2
    ) {
        data object Celsius: TemperatureUnit(
            code = TEMPERATURE_CELSIUS_CODE,
            unitTitle = R.string.temperature_celsius_name,
            unit = R.string.temperature_celsius_unit
        )
        data object Fahrenheit: TemperatureUnit(
            code = TEMPERATURE_FAHRENHEIT_CODE,
            unitTitle = R.string.temperature_fahrenheit_name,
            unit = R.string.temperature_fahrenheit_unit
        )
        data object Kelvin: TemperatureUnit(
            code = TEMPERATURE_KELVIN_CODE,
            unitTitle = R.string.temperature_kelvin_name,
            unit = R.string.temperature_kelvin_unit
        )

        companion object {
            fun getUnits(): List<TemperatureUnit> {
                return listOf(Celsius, Fahrenheit, Kelvin)
            }

            fun getByCode(code: String): TemperatureUnit {
                return when (code) {
                    TEMPERATURE_CELSIUS_CODE -> Celsius
                    TEMPERATURE_FAHRENHEIT_CODE -> Fahrenheit
                    TEMPERATURE_KELVIN_CODE -> Kelvin
                    else -> Celsius
                }
            }
        }
    }

    sealed class HumidityUnit(
        code: String,
        unitTitle: Int,
        unit: Int,
        measurementTitle: Int,
        measurementName: Int,
    ): UnitType(
        unitCode = code,
        unitTitle = unitTitle,
        unit = unit,
        measurementCode = HUMIDITY_MEASUREMENT_CODE,
        measurementTitle = measurementTitle,
        measurementName = measurementName,
        iconRes = R.drawable.icon_humidity,
        alarmType = AlarmType.HUMIDITY,
        defaultAccuracy = Accuracy.Accuracy2
    ) {
        data object Relative: HumidityUnit(
            code = HUMIDITY_RELATIVE_CODE,
            unitTitle = R.string.humidity_relative_name,
            unit = R.string.humidity_relative_unit,
            measurementTitle = R.string.relative_humidity,
            measurementName = R.string.rel_humidity
        )
        data object Absolute: HumidityUnit(
            code = HUMIDITY_ABSOLUTE_CODE,
            unitTitle = R.string.humidity_absolute_name,
            unit = R.string.humidity_absolute_unit,
            measurementTitle = R.string.absolute_humidity,
            measurementName = R.string.abs_humidity
        )
        data object DewPoint: HumidityUnit(
            code = HUMIDITY_DEW_POINT_CODE,
            unitTitle = R.string.humidity_dew_point_name,
            unit = R.string.humidity_dew_point_unit,
            measurementTitle = R.string.dewpoint,
            measurementName = R.string.dewpoint
        )

        companion object {
            fun getUnits(): List<HumidityUnit> {
                return listOf(Relative, Absolute, DewPoint)
            }

            fun getByCode(code: String): HumidityUnit {
                return when (code) {
                    HUMIDITY_RELATIVE_CODE -> Relative
                    HUMIDITY_ABSOLUTE_CODE -> Absolute
                    HUMIDITY_DEW_POINT_CODE -> DewPoint
                    else -> Relative
                }
            }
        }
    }

    sealed class PressureUnit(
        code: String,
        unitTitle: Int,
        unit: Int,
        defaultAccuracy: Accuracy
    ): UnitType(
        unitCode = code,
        unitTitle = unitTitle,
        unit = unit,
        measurementCode = PRESSURE_MEASUREMENT_CODE,
        measurementTitle = R.string.air_pressure,
        measurementName = R.string.air_pressure,
        iconRes = R.drawable.icon_pressure,
        alarmType = AlarmType.PRESSURE,
        defaultAccuracy = defaultAccuracy
    ) {
        data object Pascal: PressureUnit(
            code = PRESSURE_PASCAL_CODE,
            unitTitle = R.string.pressure_pa_name,
            unit = R.string.pressure_pa_unit,
            defaultAccuracy = Accuracy.Accuracy0
        )
        data object HectoPascal: PressureUnit(
            code = PRESSURE_HECTO_PASCAL_CODE,
            unitTitle = R.string.pressure_hpa_name,
            unit = R.string.pressure_hpa_unit,
            defaultAccuracy = Accuracy.Accuracy2
        )
        data object MmHg: PressureUnit(
            code = PRESSURE_MM_HG_CODE,
            unitTitle = R.string.pressure_mmhg_name,
            unit = R.string.pressure_mmhg_unit,
            defaultAccuracy = Accuracy.Accuracy2
        )
        data object InchHg: PressureUnit(
            code = PRESSURE_INCH_HG_CODE,
            unitTitle = R.string.pressure_inhg_name,
            unit = R.string.pressure_inhg_unit,
            defaultAccuracy = Accuracy.Accuracy2
        )

        companion object {
            fun getUnits(): List<PressureUnit> {
                return listOf(Pascal, HectoPascal, MmHg, InchHg)
            }

            fun getByCode(code: String): PressureUnit {
                return when (code) {
                    PRESSURE_PASCAL_CODE -> Pascal
                    PRESSURE_HECTO_PASCAL_CODE -> HectoPascal
                    PRESSURE_MM_HG_CODE -> MmHg
                    PRESSURE_INCH_HG_CODE -> InchHg
                    else -> HectoPascal
                }
            }
        }
    }

    sealed class MovementUnit(
        code: String,
    ): UnitType(
        unitCode = code,
        unitTitle = R.string.movements,
        unit = R.string.empty,
        measurementCode = MOVEMENT_MEASUREMENT_CODE,
        measurementTitle = R.string.movement_counter,
        measurementName = R.string.movements,
        iconRes = R.drawable.icon_movements,
        alarmType = AlarmType.MOVEMENT,
        defaultAccuracy = Accuracy.Accuracy0
    ) {
        data object MovementsCount: MovementUnit(code = MOVEMENT_COUNT)

        companion object {
            fun getByCode(code: String): UnitType {
                return when (code) {
                    MOVEMENT_COUNT -> MovementsCount
                    else -> MovementsCount
                }
            }
        }
    }

    sealed class BatteryVoltageUnit(
        code: String,
        unitTitle: Int,
        unit: Int
    ): UnitType(
        unitCode = code,
        unitTitle = unitTitle,
        unit = unit,
        iconRes = R.drawable.icon_battery,
        measurementCode = BATTERY_MEASUREMENT_CODE,
        measurementTitle = R.string.battery_voltage,
        measurementName = R.string.battery,
        defaultAccuracy = Accuracy.Accuracy2
    ) {
        data object Volt: BatteryVoltageUnit(BATTERY_UNIT_VOLT, R.string.voltage_unit, R.string.voltage_unit)
        companion object {
            fun getByCode(code: String): UnitType {
                return when (code) {
                    BATTERY_UNIT_VOLT -> Volt
                    else -> Volt
                }
            }
        }
    }

    sealed class Acceleration(
        code: String,
        title: Int,
        unit: Int,
        measurementCode: String,
        measurementTitle: Int,
        measurementName: Int,
        iconRes: Int
    ): UnitType(
        unitCode = code,
        unitTitle = title,
        unit = unit,
        measurementCode = measurementCode,
        measurementTitle = measurementTitle,
        measurementName = measurementName,
        defaultAccuracy = Accuracy.Accuracy2,
        iconRes = iconRes
    ) {
        data object GForceX: Acceleration(
            code = ACCELERATION_UNIT_GX,
            title = R.string.acceleration_x,
            unit = R.string.acceleration_unit,
            measurementCode = ACCELERATION_MEASUREMENT_CODE,
            measurementTitle = R.string.acceleration_x,
            measurementName = R.string.acc_x,
            iconRes = R.drawable.icon_acc_x
        )
        data object GForceY: Acceleration(
            code = ACCELERATION_UNIT_GY,
            title = R.string.acceleration_y,
            unit = R.string.acceleration_unit,
            measurementCode = ACCELERATION_MEASUREMENT_CODE,
            measurementTitle = R.string.acceleration_y,
            measurementName = R.string.acc_y,
            iconRes = R.drawable.icon_acc_y
        )
        data object GForceZ: Acceleration(
            code = ACCELERATION_UNIT_GZ,
            title = R.string.acceleration_z,
            unit = R.string.acceleration_unit,
            measurementCode = ACCELERATION_MEASUREMENT_CODE,
            measurementTitle = R.string.acceleration_z,
            measurementName = R.string.acc_z,
            iconRes = R.drawable.icon_acc_z
        )

        companion object {
            val units = listOf(GForceX, GForceY, GForceZ)

            fun getByCode(code: String): Acceleration {
                return when (code) {
                    ACCELERATION_UNIT_GX -> GForceX
                    ACCELERATION_UNIT_GY -> GForceY
                    ACCELERATION_UNIT_GZ -> GForceZ
                    else -> GForceX
                }
            }
        }
    }

    sealed class SignalStrengthUnit(
        code: String,
        unitTitle: Int,
        unit: Int
    ): UnitType(
        unitCode = code,
        unitTitle = unitTitle,
        unit = unit,
        measurementCode = SIGNAL_STRENGTH_MEASUREMENT_CODE,
        measurementTitle = R.string.signal_strength,
        measurementName = R.string.signal_strength,
        alarmType = AlarmType.RSSI,
        defaultAccuracy = Accuracy.Accuracy0,
        iconRes = R.drawable.icon_rssi
    ) {
        data object SignalDbm: SignalStrengthUnit(
            code = SIGNAL_STRENGTH_UNIT_DBM,
            unitTitle = R.string.signal_unit,
            unit = R.string.signal_unit
        )

        companion object {
            fun getByCode(code: String): SignalStrengthUnit {
                return when (code) {
                    SIGNAL_STRENGTH_UNIT_DBM -> SignalDbm
                    else -> SignalDbm
                }
            }
        }
    }

    sealed class AirQuality(
        code: String,
    ): UnitType(
        unitCode = code,
        unitTitle = R.string.empty,
        unit = R.string.empty,
        measurementCode = AQI_MEASUREMENT_CODE,
        iconRes = R.drawable.icon_air_quality,
        measurementTitle = R.string.air_quality,
        measurementName = R.string.air_quality,
        alarmType = AlarmType.AQI,
        defaultAccuracy = Accuracy.Accuracy0
    ) {
        data object AqiIndex: AirQuality(AQI_INDEX)

        companion object {
            fun getByCode(code: String): AirQuality {
                return when (code) {
                    AQI_INDEX -> AqiIndex
                    else -> AqiIndex
                }
            }
        }
    }

    sealed class Luminosity(
        code: String,
        unitTitle: Int,
        unit: Int
    ): UnitType(
        unitCode = code,
        unitTitle = unitTitle,
        unit = unit,
        iconRes = R.drawable.icon_luminosity,
        measurementCode = LUMINOSITY_MEASUREMENT_CODE,
        measurementTitle = R.string.luminosity,
        measurementName = R.string.light,
        alarmType = AlarmType.LUMINOSITY,
        defaultAccuracy = Accuracy.Accuracy0
    ) {
        data object Lux: Luminosity(
            code = LUMINOSITY_UNIT_LUX,
            unitTitle = R.string.unit_luminosity,
            unit = R.string.unit_luminosity
        )

        companion object {
            fun getByCode(code: String): Luminosity {
                return when (code) {
                    LUMINOSITY_UNIT_LUX -> Lux
                    else -> Lux
                }
            }
        }
    }

    sealed class SoundAvg(
        code: String,
        unitTitle: Int,
        unit: Int
    ): UnitType(
        unitCode = code,
        unitTitle = unitTitle,
        unit = unit,
        iconRes = R.drawable.icon_sound,
        measurementCode = AVG_NOISE_MEASUREMENT_CODE,
        measurementTitle = R.string.sound_avg,
        measurementName = R.string.sound_avg,
        alarmType = AlarmType.SOUND,
        defaultAccuracy = Accuracy.Accuracy0,
    ) {
        data object SoundDba: SoundAvg(
            code = NOISE_UNIT_DBA,
            unitTitle = R.string.unit_sound,
            unit = R.string.unit_sound
        )

        companion object {
            fun getByCode(code: String): SoundAvg {
                return when (code) {
                    NOISE_UNIT_DBA -> SoundDba
                    else -> SoundDba
                }
            }
        }
    }

    sealed class SoundPeak(
        code: String,
        unitTitle: Int,
        unit: Int
    ): UnitType(
        unitCode = code,
        unitTitle = unitTitle,
        unit = unit,
        iconRes = R.drawable.icon_sound,
        measurementCode = PEAK_NOISE_MEASUREMENT_CODE,
        measurementTitle = R.string.sound_peak,
        measurementName = R.string.sound_peak,
        defaultAccuracy = Accuracy.Accuracy0,
    ) {
        data object SoundDba: SoundPeak(NOISE_UNIT_DBA, R.string.unit_sound, R.string.unit_sound)

        companion object {
            fun getByCode(code: String): SoundPeak {
                return when (code) {
                    NOISE_UNIT_DBA -> SoundDba
                    else -> SoundDba
                }
            }
        }
    }

    sealed class CO2(
        code: String,
        unitTitle: Int,
        unit: Int
    ): UnitType(
        unitCode = code,
        unitTitle = unitTitle,
        unit = unit,
        iconRes = R.drawable.icon_co2,
        measurementCode = CO2_MEASUREMENT_CODE,
        measurementTitle = R.string.carbon_dioxide,
        measurementName = R.string.co2,
        alarmType = AlarmType.CO2,
        defaultAccuracy = Accuracy.Accuracy0
    ) {
        data object Ppm: CO2(
            code = CO2_UNIT_PPM,
            unitTitle = R.string.unit_co2,
            unit = R.string.unit_co2
        )

        companion object {
            fun getByCode(code: String): CO2 {
                return when (code) {
                    CO2_UNIT_PPM -> Ppm
                    else -> Ppm
                }
            }
        }
    }

    sealed class VOC(
        code: String
    ): UnitType(
        unitCode = code,
        unitTitle = R.string.empty,
        unit = R.string.empty,
        iconRes = R.drawable.icon_voc,
        measurementCode = VOC_MEASUREMENT_CODE,
        measurementTitle = R.string.volatile_organic_compounds,
        measurementName = R.string.voc_index,
        alarmType = AlarmType.VOC,
        defaultAccuracy = Accuracy.Accuracy0
    ) {
        data object VocIndex: VOC(
            code = VOC_INDEX
        )

        companion object {
            fun getByCode(code: String): VOC {
                return when (code) {
                    VOC_INDEX -> VocIndex
                    else -> VocIndex
                }
            }
        }
    }

    sealed class NOX(
        code: String
    ): UnitType(
        unitCode = code,
        unitTitle = R.string.empty,
        unit = R.string.empty,
        iconRes = R.drawable.icon_nox,
        measurementCode = NOX_MEASUREMENT_CODE,
        measurementTitle = R.string.nitrogen_oxides,
        measurementName = R.string.nox_index,
        alarmType = AlarmType.NOX,
        defaultAccuracy = Accuracy.Accuracy0
    ) {
        data object NoxIndex: NOX(code = NOX_INDEX)

        companion object {
            fun getByCode(code: String): NOX {
                return when (code) {
                    NOX_INDEX -> NoxIndex
                    else -> NoxIndex
                }
            }
        }
    }

    sealed class PM(
        measurementCode: String,
        measurementTitle: Int,
        measurementName: Int,
        iconRes: Int,
        alarmType: AlarmType? = null
    ): UnitType(
        unitCode = PM_UNIT_MGM3,
        unitTitle = R.string.unit_pm10,
        unit = R.string.unit_pm10,
        iconRes = iconRes,
        measurementCode = measurementCode,
        measurementTitle = measurementTitle,
        measurementName = measurementName,
        alarmType = alarmType,
        defaultAccuracy = Accuracy.Accuracy1
    ) {
        data object PM10: PM(
            measurementCode = PM10_MEASUREMENT_CODE,
            measurementTitle = R.string.particulate_matter_10,
            measurementName = R.string.pm10,
            iconRes = R.drawable.icon_pm10,
            alarmType = AlarmType.PM10
        )

        data object PM25: PM(
            measurementCode = PM25_MEASUREMENT_CODE,
            measurementTitle = R.string.particulate_matter_25,
            measurementName = R.string.pm25,
            iconRes = R.drawable.icon_pm25,
            alarmType = AlarmType.PM25
        )

        data object PM40: PM(
            measurementCode = PM40_MEASUREMENT_CODE,
            measurementTitle = R.string.particulate_matter_40,
            measurementName = R.string.pm40,
            iconRes = R.drawable.icon_pm40,
            alarmType = AlarmType.PM40
        )

        data object PM100: PM(
            measurementCode = PM100_MEASUREMENT_CODE,
            measurementTitle = R.string.particulate_matter_100,
            measurementName = R.string.pm100,
            iconRes = R.drawable.icon_pm100,
            alarmType = AlarmType.PM100
        )

        companion object {
            fun getByType(measurementType: String): PM {
                return when (measurementType) {
                    PM10_MEASUREMENT_CODE -> PM10
                    PM25_MEASUREMENT_CODE -> PM25
                    PM40_MEASUREMENT_CODE -> PM40
                    PM100_MEASUREMENT_CODE -> PM100
                    else -> PM25
                }
            }
        }
    }

    sealed class MsnUnit(): UnitType(
        unitCode = MSN_COUNT,
        unitTitle = R.string.empty,
        unit = R.string.empty,
        measurementCode = MSN_CODE,
        measurementTitle = R.string.measurement_sequence_number,
        measurementName = R.string.meas_seq_number,
        iconRes = R.drawable.icon_msn,
        defaultAccuracy = Accuracy.Accuracy0
    ) {
        data object MsnCount: MsnUnit()

        companion object {
            fun getByCode(code: String): UnitType {
                return when (code) {
                    MSN_COUNT -> MsnCount
                    else -> MsnCount
                }
            }
        }
    }

    companion object {
        const val TEMPERATURE_MEASUREMENT_CODE = "TEMPERATURE"
        const val TEMPERATURE_CELSIUS_CODE = "C"
        const val TEMPERATURE_FAHRENHEIT_CODE = "F"
        const val TEMPERATURE_KELVIN_CODE = "K"

        const val HUMIDITY_MEASUREMENT_CODE = "HUMIDITY"
        const val HUMIDITY_RELATIVE_CODE = "0"
        const val HUMIDITY_ABSOLUTE_CODE = "1"
        const val HUMIDITY_DEW_POINT_CODE = "2"

        const val PRESSURE_MEASUREMENT_CODE = "PRESSURE"
        const val PRESSURE_PASCAL_CODE = "0"
        const val PRESSURE_HECTO_PASCAL_CODE = "1"
        const val PRESSURE_MM_HG_CODE = "2"
        const val PRESSURE_INCH_HG_CODE = "3"

        const val MOVEMENT_MEASUREMENT_CODE = "MOVEMENT"
        const val MOVEMENT_COUNT = "COUNT"

        const val BATTERY_MEASUREMENT_CODE = "BATTERY"
        const val BATTERY_UNIT_VOLT = "VOLT"

        const val ACCELERATION_MEASUREMENT_CODE = "ACCELERATION"
        const val ACCELERATION_UNIT_GX = "GX"
        const val ACCELERATION_UNIT_GY = "GY"
        const val ACCELERATION_UNIT_GZ = "GZ"

        const val SIGNAL_STRENGTH_MEASUREMENT_CODE = "SIGNAL"
        const val SIGNAL_STRENGTH_UNIT_DBM = "DBM"

        const val AQI_MEASUREMENT_CODE = "AQI"
        const val AQI_INDEX = "INDEX"

        const val LUMINOSITY_MEASUREMENT_CODE = "LUMINOSITY"
        const val LUMINOSITY_UNIT_LUX = "LX"

        const val AVG_NOISE_MEASUREMENT_CODE = "SOUNDAVG"
        const val PEAK_NOISE_MEASUREMENT_CODE = "SOUNDPEAK"
        const val NOISE_UNIT_DBA = "DBA"

        const val CO2_MEASUREMENT_CODE = "CO2"
        const val CO2_UNIT_PPM = "PPM"

        const val VOC_MEASUREMENT_CODE = "VOC"
        const val VOC_INDEX = "INDEX"

        const val NOX_MEASUREMENT_CODE = "NOX"
        const val NOX_INDEX = "INDEX"

        const val PM10_MEASUREMENT_CODE = "PM10"
        const val PM25_MEASUREMENT_CODE = "PM25"
        const val PM40_MEASUREMENT_CODE = "PM40"
        const val PM100_MEASUREMENT_CODE = "PM100"
        const val PM_UNIT_MGM3 = "MGM3"

        const val MSN_CODE = "MSN"
        const val MSN_COUNT = "COUNT"

        fun getByCode(code: String): UnitType? {
            val codeParts = code.split("_")
            if (codeParts.size != 2) return null
            val measurementType = codeParts[0]
            val unitType = codeParts[1]
            return when (measurementType) {
                TEMPERATURE_MEASUREMENT_CODE -> TemperatureUnit.getByCode(unitType)
                HUMIDITY_MEASUREMENT_CODE -> HumidityUnit.getByCode(unitType)
                PRESSURE_MEASUREMENT_CODE -> PressureUnit.getByCode(unitType)
                MOVEMENT_MEASUREMENT_CODE -> MovementUnit.getByCode(unitType)
                BATTERY_MEASUREMENT_CODE -> BatteryVoltageUnit.getByCode(unitType)
                ACCELERATION_MEASUREMENT_CODE -> Acceleration.getByCode(unitType)
                SIGNAL_STRENGTH_MEASUREMENT_CODE -> SignalStrengthUnit.getByCode(unitType)
                AQI_MEASUREMENT_CODE -> AirQuality.getByCode(unitType)
                LUMINOSITY_MEASUREMENT_CODE -> Luminosity.getByCode(unitType)
                AVG_NOISE_MEASUREMENT_CODE -> SoundAvg.getByCode(unitType)
                PEAK_NOISE_MEASUREMENT_CODE -> SoundPeak.getByCode(unitType)
                CO2_MEASUREMENT_CODE -> CO2.getByCode(unitType)
                VOC_MEASUREMENT_CODE -> VOC.getByCode(unitType)
                NOX_MEASUREMENT_CODE -> NOX.getByCode(unitType)
                PM10_MEASUREMENT_CODE, PM25_MEASUREMENT_CODE, PM40_MEASUREMENT_CODE, PM100_MEASUREMENT_CODE -> PM.getByType(measurementType)
                MSN_CODE -> MsnUnit.getByCode(unitType)
                else -> null
            }
        }

        fun getListOfUnits(codesList: List<String>): List<UnitType> {
            val unitsList = mutableListOf<UnitType>()
            for (code in codesList) {
                getByCode(code)?.let { unitType ->
                    unitsList.add(unitType)
                }
            }
            return unitsList
        }
    }
}

fun UnitType.getDescriptionBodyResId(): Int {
    return when (this) {
        is UnitType.TemperatureUnit.Celsius -> R.string.description_text_temperature_celsius
        is UnitType.TemperatureUnit.Fahrenheit -> R.string.description_text_temperature_fahrenheit
        is UnitType.TemperatureUnit.Kelvin -> R.string.description_text_temperature_kelvin
        is UnitType.HumidityUnit.Relative -> R.string.description_text_humidity_relative
        is UnitType.HumidityUnit.Absolute -> R.string.description_text_humidity_absolute
        is UnitType.HumidityUnit.DewPoint -> R.string.description_text_humidity_dewpoint
        is UnitType.PressureUnit -> R.string.description_text_pressure
        is UnitType.MovementUnit -> R.string.description_text_movement
        is UnitType.BatteryVoltageUnit -> R.string.description_text_battery_voltage
        is UnitType.Acceleration -> R.string.description_text_acceleration
        is UnitType.SignalStrengthUnit -> R.string.description_text_signal_strength
        is UnitType.AirQuality -> R.string.description_text_air_quality
        is UnitType.Luminosity -> R.string.description_text_luminosity
        is UnitType.SoundAvg -> R.string.description_text_sound_level
        is UnitType.SoundPeak -> R.string.description_text_sound_level
        is UnitType.CO2 -> R.string.description_text_co2
        is UnitType.VOC -> R.string.description_text_voc
        is UnitType.NOX -> R.string.description_text_nox
        is UnitType.PM.PM10 -> R.string.description_text_pm
        is UnitType.PM.PM25 -> R.string.description_text_pm
        is UnitType.PM.PM40 -> R.string.description_text_pm
        is UnitType.PM.PM100 -> R.string.description_text_pm
        UnitType.MsnUnit.MsnCount -> R.string.description_text_measurement_sequence_number
    }
}