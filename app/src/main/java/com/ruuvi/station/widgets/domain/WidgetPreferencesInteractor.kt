package com.ruuvi.station.widgets.domain

import android.content.Context
import android.content.SharedPreferences

class WidgetPreferencesInteractor(val context: Context) {
    private val sharedPreferences: SharedPreferences by lazy { context.getSharedPreferences(PREFS_NAME, 0) }

    fun saveWidgetSettings(appWidgetId: Int, sensorId: String) {
        sharedPreferences.edit().putString("$PREF_WIDGET_PREFIX$appWidgetId", sensorId).apply()
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
    }
}