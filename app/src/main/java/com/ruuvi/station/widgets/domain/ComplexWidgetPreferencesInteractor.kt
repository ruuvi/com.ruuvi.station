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
            val deserialized = Json{ignoreUnknownKeys = true}.decodeFromString<List<ComplexWidgetPreferenceItem>>(prefString)
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
        checkedHumidity = item.checkedHumidity,
        checkedPressure = item.checkedPressure,
        checkedMovement = item.checkedMovement,
        checkedVoltage = item.checkedVoltage,
        checkedSignalStrength = item.checkedSignalStrength,
        checkedAccelerationX = item.checkedAccelerationX,
        checkedAccelerationY = item.checkedAccelerationY,
        checkedAccelerationZ = item.checkedAccelerationZ,
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
}