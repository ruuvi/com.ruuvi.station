package com.ruuvi.station.widgets.domain

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.widgets.complexWidget.ComplexWidgetSensorItem
import com.ruuvi.station.widgets.data.WidgetType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.lang.Exception

class ComplexWidgetPreferencesInteractor(
    val context: Context,
    private val preferencesRepository: PreferencesRepository =
        PreferencesRepository(Preferences(context)),
) {
    private val sharedPreferences: SharedPreferences by lazy { context.getSharedPreferences(
        PREFS_NAME, 0) }
    private val json = Json { ignoreUnknownKeys = true }

    fun saveComplexWidgetSettings(appWidgetId: Int, items: List<ComplexWidgetSensorItem>) {
        val list = items
            .filter { it.checked && it.anySensorChecked() }
            .map(::ComplexWidgetPreferenceItem)
        persistComplexWidgetSettings(appWidgetId, list)
    }

    private fun persistComplexWidgetSettings(
        appWidgetId: Int,
        items: List<ComplexWidgetPreferenceItem>,
    ) {
        val serialized = json.encodeToString(items.map(::StoredComplexWidgetPreferenceItem))
        sharedPreferences
            .edit {
                putString("$PREF_WIDGET_PREFIX$appWidgetId", serialized)
                putInt("$PREF_WIDGET_SCHEMA$appWidgetId", CURRENT_SCHEMA_VERSION)
            }
    }

    fun getComplexWidgetSettings(appWidgetId: Int): List<ComplexWidgetPreferenceItem> {
        val prefString = sharedPreferences.getString("$PREF_WIDGET_PREFIX$appWidgetId", "") ?: ""
        if (prefString.isBlank()) return emptyList()

        return try {
            val storedItems = json.decodeFromString<List<StoredComplexWidgetPreferenceItem>>(prefString)
            val schemaKey = "$PREF_WIDGET_SCHEMA$appWidgetId"
            val needsMigration =
                sharedPreferences.getInt(schemaKey, LEGACY_SCHEMA_VERSION) < CURRENT_SCHEMA_VERSION
            val decodedItems = if (needsMigration) {
                storedItems.map { stored ->
                    migrateLegacyComplexWidgetPreference(
                        item = stored.toPreferenceItem(),
                        legacySoundRealTime = stored.legacySoundRealTime,
                        temperatureUnit = preferencesRepository.getTemperatureUnit(),
                        humidityUnit = preferencesRepository.getHumidityUnit(),
                        pressureUnit = preferencesRepository.getPressureUnit(),
                    )
                }
            } else {
                storedItems.map { it.toPreferenceItem() }
            }
            if (needsMigration) {
                persistComplexWidgetSettings(appWidgetId, decodedItems)
            }
            decodedItems
        } catch (e: Exception) {
            Timber.e(e)
            listOf()
        }
    }

    fun removeComplexWidgetSettings(appWidgetId: Int) {
        sharedPreferences.edit {
            remove("$PREF_WIDGET_PREFIX$appWidgetId")
            remove("$PREF_WIDGET_SCHEMA$appWidgetId")
        }
    }

    companion object {
        private const val PREF_WIDGET_PREFIX = "ruuvi_complex_widget_"
        private const val PREF_WIDGET_SCHEMA = "ruuvi_complex_widget_schema_"
        private const val PREFS_NAME = "com.ruuvi.station.widgets.complexWidget"
        private const val LEGACY_SCHEMA_VERSION = 0
        internal const val CURRENT_SCHEMA_VERSION = 1

        internal fun migrateLegacyComplexWidgetPreference(
            item: ComplexWidgetPreferenceItem,
            legacySoundRealTime: Boolean,
            temperatureUnit: UnitType.TemperatureUnit,
            humidityUnit: UnitType.HumidityUnit,
            pressureUnit: UnitType.PressureUnit,
        ): ComplexWidgetPreferenceItem {
            val legacyTemperatureSelected = item.checkedTemperature
            val legacyHumiditySelected = item.checkedHumidity
            val legacyPressureSelected = item.checkedPressure

            return item.copy(
                checkedTemperature = legacyTemperatureSelected &&
                    temperatureUnit == UnitType.TemperatureUnit.Celsius,
                checkedTemperatureF = item.checkedTemperatureF ||
                    legacyTemperatureSelected &&
                    temperatureUnit == UnitType.TemperatureUnit.Fahrenheit,
                checkedTemperatureK = item.checkedTemperatureK ||
                    legacyTemperatureSelected &&
                    temperatureUnit == UnitType.TemperatureUnit.Kelvin,
                checkedHumidity = legacyHumiditySelected &&
                    humidityUnit == UnitType.HumidityUnit.Relative,
                checkedHumidityAbsolute = item.checkedHumidityAbsolute ||
                    legacyHumiditySelected &&
                    humidityUnit == UnitType.HumidityUnit.Absolute,
                checkedDewPointC = item.checkedDewPointC ||
                    legacyHumiditySelected &&
                    humidityUnit == UnitType.HumidityUnit.DewPoint &&
                    temperatureUnit == UnitType.TemperatureUnit.Celsius,
                checkedDewPointF = item.checkedDewPointF ||
                    legacyHumiditySelected &&
                    humidityUnit == UnitType.HumidityUnit.DewPoint &&
                    temperatureUnit == UnitType.TemperatureUnit.Fahrenheit,
                checkedDewPointK = item.checkedDewPointK ||
                    legacyHumiditySelected &&
                    humidityUnit == UnitType.HumidityUnit.DewPoint &&
                    temperatureUnit == UnitType.TemperatureUnit.Kelvin,
                checkedPressure = legacyPressureSelected &&
                    pressureUnit == UnitType.PressureUnit.HectoPascal,
                checkedPressurePa = item.checkedPressurePa ||
                    legacyPressureSelected &&
                    pressureUnit == UnitType.PressureUnit.Pascal,
                checkedPressureMmHg = item.checkedPressureMmHg ||
                    legacyPressureSelected &&
                    pressureUnit == UnitType.PressureUnit.MmHg,
                checkedPressureInHg = item.checkedPressureInHg ||
                    legacyPressureSelected &&
                    pressureUnit == UnitType.PressureUnit.InchHg,
                checkedSoundAverage = item.checkedSoundAverage || legacySoundRealTime,
            )
        }
    }
}

data class ComplexWidgetPreferenceItem(
    val sensorId: String,
    val checkedTemperature: Boolean = false,
    val checkedTemperatureF: Boolean = false,
    val checkedTemperatureK: Boolean = false,
    val checkedHumidity: Boolean = false,
    val checkedHumidityAbsolute: Boolean = false,
    val checkedDewPointC: Boolean = false,
    val checkedDewPointF: Boolean = false,
    val checkedDewPointK: Boolean = false,
    val checkedPressure: Boolean = false,
    val checkedPressurePa: Boolean = false,
    val checkedPressureMmHg: Boolean = false,
    val checkedPressureInHg: Boolean = false,
    val checkedMovement: Boolean = false,
    val checkedVoltage: Boolean = false,
    val checkedSignalStrength: Boolean = false,
    val checkedAccelerationX: Boolean = false,
    val checkedAccelerationY: Boolean = false,
    val checkedAccelerationZ: Boolean = false,
    val checkedSoundAverage: Boolean = false,
    val checkedSoundPeak: Boolean = false,
    val checkedMsn: Boolean = false,
    val checkedAQI: Boolean = false,
    val checkedLuminosity: Boolean = false,
    val checkedCO2: Boolean = false,
    val checkedVOC: Boolean = false,
    val checkedNOX: Boolean = false,
    val checkedPM10: Boolean = false,
    val checkedPM25: Boolean = false,
    val checkedPM40: Boolean = false,
    val checkedPM100: Boolean = false,
) {
    constructor(item: ComplexWidgetSensorItem): this (
        sensorId = item.sensor.id,
        checkedTemperature = item.checkedTemperature,
        checkedTemperatureF = item.checkedTemperatureF,
        checkedTemperatureK = item.checkedTemperatureK,
        checkedHumidity = item.checkedHumidity,
        checkedHumidityAbsolute = item.checkedHumidityAbsolute,
        checkedDewPointC = item.checkedDewPointC,
        checkedDewPointF = item.checkedDewPointF,
        checkedDewPointK = item.checkedDewPointK,
        checkedPressure = item.checkedPressure,
        checkedPressurePa = item.checkedPressurePa,
        checkedPressureMmHg = item.checkedPressureMmHg,
        checkedPressureInHg = item.checkedPressureInHg,
        checkedMovement = item.checkedMovement,
        checkedVoltage = item.checkedVoltage,
        checkedSignalStrength = item.checkedSignalStrength,
        checkedAccelerationX = item.checkedAccelerationX,
        checkedAccelerationY = item.checkedAccelerationY,
        checkedAccelerationZ = item.checkedAccelerationZ,
        checkedSoundAverage = item.checkedSoundAverage,
        checkedSoundPeak = item.checkedSoundPeak,
        checkedMsn = item.checkedMsn,
        checkedAQI = item.checkedAQI,
        checkedLuminosity = item.checkedLuminosity,
        checkedCO2 = item.checkedCO2,
        checkedVOC = item.checkedVOC,
        checkedNOX = item.checkedNOX,
        checkedPM10 = item.checkedPM10,
        checkedPM25 = item.checkedPM25,
        checkedPM40 = item.checkedPM40,
        checkedPM100 = item.checkedPM100,
    )

    internal fun isChecked(widgetType: WidgetType): Boolean = when (widgetType) {
        WidgetType.TEMPERATURE -> checkedTemperature
        WidgetType.TEMPERATURE_F -> checkedTemperatureF
        WidgetType.TEMPERATURE_K -> checkedTemperatureK
        WidgetType.HUMIDITY -> checkedHumidity
        WidgetType.HUMIDITY_ABSOLUTE -> checkedHumidityAbsolute
        WidgetType.DEW_POINT_C -> checkedDewPointC
        WidgetType.DEW_POINT_F -> checkedDewPointF
        WidgetType.DEW_POINT_K -> checkedDewPointK
        WidgetType.PRESSURE -> checkedPressure
        WidgetType.PRESSURE_PA -> checkedPressurePa
        WidgetType.PRESSURE_MMHG -> checkedPressureMmHg
        WidgetType.PRESSURE_INHG -> checkedPressureInHg
        WidgetType.MOVEMENT -> checkedMovement
        WidgetType.VOLTAGE -> checkedVoltage
        WidgetType.SIGNAL_STRENGTH -> checkedSignalStrength
        WidgetType.ACCELERATION_X -> checkedAccelerationX
        WidgetType.ACCELERATION_Y -> checkedAccelerationY
        WidgetType.ACCELERATION_Z -> checkedAccelerationZ
        WidgetType.SOUND_AVERAGE -> checkedSoundAverage
        WidgetType.SOUND_PEAK -> checkedSoundPeak
        WidgetType.MEASUREMENT_SEQUENCE_NUMBER -> checkedMsn
        WidgetType.AIR_QUALITY -> checkedAQI
        WidgetType.LUMINOSITY -> checkedLuminosity
        WidgetType.CO2 -> checkedCO2
        WidgetType.VOC -> checkedVOC
        WidgetType.NOX -> checkedNOX
        WidgetType.PM10 -> checkedPM10
        WidgetType.PM25 -> checkedPM25
        WidgetType.PM40 -> checkedPM40
        WidgetType.PM100 -> checkedPM100
    }
}

@Serializable
private data class StoredComplexWidgetPreferenceItem(
    val sensorId: String,
    val checkedTemperature: Boolean,
    val checkedTemperatureF: Boolean = false,
    val checkedTemperatureK: Boolean = false,
    val checkedHumidity: Boolean,
    val checkedHumidityAbsolute: Boolean = false,
    val checkedDewPointC: Boolean = false,
    val checkedDewPointF: Boolean = false,
    val checkedDewPointK: Boolean = false,
    val checkedPressure: Boolean,
    val checkedPressurePa: Boolean = false,
    val checkedPressureMmHg: Boolean = false,
    val checkedPressureInHg: Boolean = false,
    val checkedMovement: Boolean,
    val checkedVoltage: Boolean,
    val checkedSignalStrength: Boolean,
    val checkedAccelerationX: Boolean,
    val checkedAccelerationY: Boolean,
    val checkedAccelerationZ: Boolean,
    @SerialName("checkedSoundRealTime")
    val legacySoundRealTime: Boolean = false,
    val checkedSoundAverage: Boolean = false,
    val checkedSoundPeak: Boolean = false,
    val checkedMsn: Boolean = false,
    val checkedAQI: Boolean = false,
    val checkedLuminosity: Boolean = false,
    val checkedCO2: Boolean = false,
    val checkedVOC: Boolean = false,
    val checkedNOX: Boolean = false,
    val checkedPM10: Boolean = false,
    val checkedPM25: Boolean = false,
    val checkedPM40: Boolean = false,
    val checkedPM100: Boolean = false,
) {
    constructor(item: ComplexWidgetPreferenceItem) : this(
        sensorId = item.sensorId,
        checkedTemperature = item.checkedTemperature,
        checkedTemperatureF = item.checkedTemperatureF,
        checkedTemperatureK = item.checkedTemperatureK,
        checkedHumidity = item.checkedHumidity,
        checkedHumidityAbsolute = item.checkedHumidityAbsolute,
        checkedDewPointC = item.checkedDewPointC,
        checkedDewPointF = item.checkedDewPointF,
        checkedDewPointK = item.checkedDewPointK,
        checkedPressure = item.checkedPressure,
        checkedPressurePa = item.checkedPressurePa,
        checkedPressureMmHg = item.checkedPressureMmHg,
        checkedPressureInHg = item.checkedPressureInHg,
        checkedMovement = item.checkedMovement,
        checkedVoltage = item.checkedVoltage,
        checkedSignalStrength = item.checkedSignalStrength,
        checkedAccelerationX = item.checkedAccelerationX,
        checkedAccelerationY = item.checkedAccelerationY,
        checkedAccelerationZ = item.checkedAccelerationZ,
        checkedSoundAverage = item.checkedSoundAverage,
        checkedSoundPeak = item.checkedSoundPeak,
        checkedMsn = item.checkedMsn,
        checkedAQI = item.checkedAQI,
        checkedLuminosity = item.checkedLuminosity,
        checkedCO2 = item.checkedCO2,
        checkedVOC = item.checkedVOC,
        checkedNOX = item.checkedNOX,
        checkedPM10 = item.checkedPM10,
        checkedPM25 = item.checkedPM25,
        checkedPM40 = item.checkedPM40,
        checkedPM100 = item.checkedPM100,
    )

    fun toPreferenceItem() = ComplexWidgetPreferenceItem(
        sensorId = sensorId,
        checkedTemperature = checkedTemperature,
        checkedTemperatureF = checkedTemperatureF,
        checkedTemperatureK = checkedTemperatureK,
        checkedHumidity = checkedHumidity,
        checkedHumidityAbsolute = checkedHumidityAbsolute,
        checkedDewPointC = checkedDewPointC,
        checkedDewPointF = checkedDewPointF,
        checkedDewPointK = checkedDewPointK,
        checkedPressure = checkedPressure,
        checkedPressurePa = checkedPressurePa,
        checkedPressureMmHg = checkedPressureMmHg,
        checkedPressureInHg = checkedPressureInHg,
        checkedMovement = checkedMovement,
        checkedVoltage = checkedVoltage,
        checkedSignalStrength = checkedSignalStrength,
        checkedAccelerationX = checkedAccelerationX,
        checkedAccelerationY = checkedAccelerationY,
        checkedAccelerationZ = checkedAccelerationZ,
        checkedSoundAverage = checkedSoundAverage,
        checkedSoundPeak = checkedSoundPeak,
        checkedMsn = checkedMsn,
        checkedAQI = checkedAQI,
        checkedLuminosity = checkedLuminosity,
        checkedCO2 = checkedCO2,
        checkedVOC = checkedVOC,
        checkedNOX = checkedNOX,
        checkedPM10 = checkedPM10,
        checkedPM25 = checkedPM25,
        checkedPM40 = checkedPM40,
        checkedPM100 = checkedPM100,
    )
}
