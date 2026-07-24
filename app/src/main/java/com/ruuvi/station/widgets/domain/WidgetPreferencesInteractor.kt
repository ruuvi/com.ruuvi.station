package com.ruuvi.station.widgets.domain

import android.content.Context
import android.content.SharedPreferences
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.widgets.data.WidgetType

class WidgetPreferencesInteractor(
    val context: Context,
    private val preferencesRepository: PreferencesRepository =
        PreferencesRepository(Preferences(context)),
) {
    private val sharedPreferences: SharedPreferences by lazy { context.getSharedPreferences(PREFS_NAME, 0) }

    fun saveSimpleWidgetSettings(appWidgetId: Int, sensorId: String, widgetType: WidgetType) {
        sharedPreferences
            .edit()
            .putString("$PREF_SIMPLE_WIDGET_SENSOR$appWidgetId", sensorId)
            .putInt("$PREF_SIMPLE_WIDGET_TYPE$appWidgetId", widgetType.code)
            .putInt("$PREF_SIMPLE_WIDGET_SCHEMA$appWidgetId", CURRENT_SCHEMA_VERSION)
            .apply()
    }

    fun getSimpleWidgetSensor(appWidgetId: Int): String? {
        return sharedPreferences.getString("$PREF_SIMPLE_WIDGET_SENSOR$appWidgetId", null)
    }

    fun getSimpleWidgetType(appWidgetId: Int): WidgetType {
        val typeKey = "$PREF_SIMPLE_WIDGET_TYPE$appWidgetId"
        val storedCode = sharedPreferences.getInt(typeKey, -1)
        val schemaKey = "$PREF_SIMPLE_WIDGET_SCHEMA$appWidgetId"

        if (sharedPreferences.contains(typeKey) &&
            sharedPreferences.getInt(schemaKey, LEGACY_SCHEMA_VERSION) < CURRENT_SCHEMA_VERSION
        ) {
            val migratedType = migrateLegacyWidgetType(
                storedCode = storedCode,
                temperatureUnit = preferencesRepository.getTemperatureUnit(),
                humidityUnit = preferencesRepository.getHumidityUnit(),
                pressureUnit = preferencesRepository.getPressureUnit(),
            )
            sharedPreferences
                .edit()
                .putInt(typeKey, migratedType.code)
                .putInt(schemaKey, CURRENT_SCHEMA_VERSION)
                .apply()
            return migratedType
        }

        return WidgetType.getByCode(storedCode)
    }

    fun removeSimpleWidgetSettings(appWidgetId: Int) {
        sharedPreferences
            .edit()
            .remove("$PREF_SIMPLE_WIDGET_SENSOR$appWidgetId")
            .remove("$PREF_SIMPLE_WIDGET_TYPE$appWidgetId")
            .remove("$PREF_SIMPLE_WIDGET_SCHEMA$appWidgetId")
            .remove("$PREF_WIDGET_PREFIX$appWidgetId")
            .apply()
    }

    companion object {
        private const val PREF_WIDGET_PREFIX = "ruuvi_widget_"
        private const val PREFS_NAME = "com.ruuvi.station.widgets.ui.FirstWidget"

        private const val PREF_SIMPLE_WIDGET_SENSOR = "ruuvi_simple_widget_sensor_"
        private const val PREF_SIMPLE_WIDGET_TYPE = "ruuvi_simple_widget_type_"
        private const val PREF_SIMPLE_WIDGET_SCHEMA = "ruuvi_simple_widget_schema_"
        private const val LEGACY_SCHEMA_VERSION = 0
        internal const val CURRENT_SCHEMA_VERSION = 1

        internal fun migrateLegacyWidgetType(
            storedCode: Int,
            temperatureUnit: UnitType.TemperatureUnit,
            humidityUnit: UnitType.HumidityUnit,
            pressureUnit: UnitType.PressureUnit,
        ): WidgetType = when (storedCode) {
            WidgetType.TEMPERATURE.code -> when (temperatureUnit) {
                UnitType.TemperatureUnit.Celsius -> WidgetType.TEMPERATURE
                UnitType.TemperatureUnit.Fahrenheit -> WidgetType.TEMPERATURE_F
                UnitType.TemperatureUnit.Kelvin -> WidgetType.TEMPERATURE_K
            }
            WidgetType.HUMIDITY.code -> when (humidityUnit) {
                UnitType.HumidityUnit.Relative -> WidgetType.HUMIDITY
                UnitType.HumidityUnit.Absolute -> WidgetType.HUMIDITY_ABSOLUTE
                UnitType.HumidityUnit.DewPoint -> when (temperatureUnit) {
                    UnitType.TemperatureUnit.Celsius -> WidgetType.DEW_POINT_C
                    UnitType.TemperatureUnit.Fahrenheit -> WidgetType.DEW_POINT_F
                    UnitType.TemperatureUnit.Kelvin -> WidgetType.DEW_POINT_K
                }
            }
            WidgetType.PRESSURE.code -> when (pressureUnit) {
                UnitType.PressureUnit.Pascal -> WidgetType.PRESSURE_PA
                UnitType.PressureUnit.HectoPascal -> WidgetType.PRESSURE
                UnitType.PressureUnit.MmHg -> WidgetType.PRESSURE_MMHG
                UnitType.PressureUnit.InchHg -> WidgetType.PRESSURE_INHG
            }
            WidgetType.LEGACY_SOUND_REAL_TIME_CODE -> WidgetType.SOUND_AVERAGE
            else -> WidgetType.getByCode(storedCode)
        }
    }
}
