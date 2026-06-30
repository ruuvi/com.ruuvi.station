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
import androidx.core.content.edit

class ComplexWidgetPreferencesInteractor(val context: Context) {
    private val sharedPreferences: SharedPreferences by lazy { context.getSharedPreferences(
        PREFS_NAME, 0) }

    fun saveComplexWidgetSettings(appWidgetId: Int, items: List<ComplexWidgetSensorItem>) {
        val list = items.filter { it.checked && it.anySensorChecked() }.map { ComplexWidgetPreferenceItem(it) }
        val serialized = Json.encodeToString(list)
        sharedPreferences
            .edit {
                putString("$PREF_WIDGET_PREFIX$appWidgetId", serialized)
            }
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
    val checkedSoundRealTime: Boolean = false,
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
        checkedSoundRealTime = item.checkedSoundRealTime,
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
}
