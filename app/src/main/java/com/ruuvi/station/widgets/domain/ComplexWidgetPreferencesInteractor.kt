package com.ruuvi.station.widgets.domain

import android.content.Context
import android.content.SharedPreferences
import com.ruuvi.station.widgets.complexWidget.ComplexWidgetSensorItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.lang.Exception

class ComplexWidgetPreferencesInteractor(val context: Context) {
    private val sharedPreferences: SharedPreferences by lazy { context.getSharedPreferences(
        PREFS_NAME, 0) }

    fun saveComplexWidgetSettings(appWidgetId: Int, items: List<ComplexWidgetSensorItem>) {
        val list = items.filter { it.checked && it.anySensorChecked() }.map { ComplexWidgetPreferenceItem(it) }
        val serialized = Json.encodeToString(list)
        sharedPreferences
            .edit()
            .putString("$PREF_WIDGET_PREFIX$appWidgetId", serialized)
            .apply()
    }

    fun getComplexWidgetSettings(appWidgetId: Int): List<ComplexWidgetPreferenceItem> {
        val prefString = sharedPreferences.getString("$PREF_WIDGET_PREFIX$appWidgetId", "") ?: ""
        return try {
            val deserialized = Json.decodeFromString<List<ComplexWidgetPreferenceItem>>(prefString)
            deserialized
        } catch (e: Exception) {
            Timber.e(e)
            listOf()
        }
    }

    companion object {
        private const val PREF_WIDGET_PREFIX = "ruuvi_complex_widget_"
        private const val PREFS_NAME = "com.ruuvi.station.widgets.complexWidget"
    }
}

@Serializable
data class ComplexWidgetPreferenceItem(
    val sensorId: String,
    val checkedTemperature: Boolean,
    val checkedHumidity: Boolean,
    val checkedPressure: Boolean,
    val checkedMovement: Boolean,
    val checkedVoltage: Boolean,
    val checkedSignalStrength: Boolean,
    val checkedAccelerationX: Boolean,
    val checkedAccelerationY: Boolean,
    val checkedAccelerationZ: Boolean,
) {
    constructor(item: ComplexWidgetSensorItem): this (
        item.sensorId,
        item.checkedTemperature,
        item.checkedHumidity,
        item.checkedPressure,
        item.checkedMovement,
        item.checkedVoltage,
        item.checkedSignalStrength,
        item.checkedAccelerationX,
        item.checkedAccelerationY,
        item.checkedAccelerationZ,
    )
}