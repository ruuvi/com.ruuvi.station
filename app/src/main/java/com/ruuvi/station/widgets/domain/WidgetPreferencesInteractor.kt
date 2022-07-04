package com.ruuvi.station.widgets.domain

import android.content.Context
import android.content.SharedPreferences
import com.ruuvi.station.widgets.data.WidgetType

class WidgetPreferencesInteractor(val context: Context) {
    private val sharedPreferences: SharedPreferences by lazy { context.getSharedPreferences(PREFS_NAME, 0) }

    fun saveWidgetSettings(appWidgetId: Int, sensorId: String) {
        sharedPreferences.edit().putString("$PREF_WIDGET_PREFIX$appWidgetId", sensorId).apply()
    }

    fun saveSimpleWidgetSettings(appWidgetId: Int, sensorId: String, widgetType: WidgetType) {
        sharedPreferences
            .edit()
            .putString("$PREF_SIMPLE_WIDGET_SENSOR$appWidgetId", sensorId)
            .putInt("$PREF_SIMPLE_WIDGET_TYPE$appWidgetId", widgetType.code)
            .apply()
    }

    fun getSimpleWidgetSensor(appWidgetId: Int): String? {
        return sharedPreferences.getString("$PREF_SIMPLE_WIDGET_SENSOR$appWidgetId", null)
    }

    fun getSimpleWidgetType(appWidgetId: Int): WidgetType {
        val widgetTypeCode = sharedPreferences.getInt("$PREF_SIMPLE_WIDGET_TYPE$appWidgetId", -1)
        return WidgetType.getByCode(widgetTypeCode)
    }

    fun removeSimpleWidgetSettings(appWidgetId: Int) {
        sharedPreferences
            .edit()
            .remove("$PREF_SIMPLE_WIDGET_SENSOR$appWidgetId")
            .remove("$PREF_SIMPLE_WIDGET_TYPE$appWidgetId")
            .apply()
    }

    fun getWidgetSensor(appWidgetId: Int): String? {
        return sharedPreferences.getString("$PREF_WIDGET_PREFIX$appWidgetId", null)
    }

    fun removeWidgetSensor(appWidgetId: Int) {
        sharedPreferences.edit().remove("$PREF_WIDGET_PREFIX$appWidgetId")
    }

    companion object {
        private const val PREF_WIDGET_PREFIX = "ruuvi_widget_"
        private const val PREFS_NAME = "com.ruuvi.station.widgets.ui.FirstWidget"

        private const val PREF_SIMPLE_WIDGET_SENSOR = "ruuvi_simple_widget_sensor_"
        private const val PREF_SIMPLE_WIDGET_TYPE = "ruuvi_simple_widget_type_"
    }
}