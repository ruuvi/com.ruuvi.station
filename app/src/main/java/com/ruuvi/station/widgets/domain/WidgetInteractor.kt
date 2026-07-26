package com.ruuvi.station.widgets.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.util.extensions.diffGreaterThan
import com.ruuvi.station.util.extensions.hours24
import com.ruuvi.station.util.extensions.localizedDate
import com.ruuvi.station.util.extensions.localizedTime
import com.ruuvi.station.widgets.data.ComplexWidgetData
import com.ruuvi.station.widgets.data.SimpleWidgetData
import com.ruuvi.station.widgets.data.WidgetSensorSnapshot
import com.ruuvi.station.widgets.data.WidgetType
import java.util.Date

class WidgetInteractor internal constructor(
    private val context: Context,
    private val snapshotProvider: WidgetSensorSnapshotProvider,
    private val measurementFormatter: WidgetMeasurementFormatterRegistry,
) {
    fun getCloudSensorsList() = snapshotProvider.getFavoriteSensors()

    suspend fun getComplexWidgetData(
        sensorId: String,
        settings: ComplexWidgetPreferenceItem?,
    ): ComplexWidgetData {
        val sensor = snapshotProvider.getFavoriteSensor(sensorId)
            ?: return emptyComplexResult(sensorId)
        val snapshot = snapshotProvider.getLatestSnapshot(sensor)
            ?: return emptyComplexResult(sensorId, sensor.displayName)
        val selectedTypes = WidgetType.filterWidgetTypes(sensor)
            .filter { type -> settings?.isChecked(type) == true }

        return ComplexWidgetData(
            sensorId = snapshot.sensorId,
            timestamp = snapshot.timestamp,
            displayName = snapshot.displayName,
            sensorValues = measurementFormatter.format(selectedTypes, snapshot),
            updated = snapshot.formattedTimestamp(),
        )
    }

    suspend fun getSimpleWidgetData(
        sensorId: String,
        widgetType: WidgetType,
    ): SimpleWidgetData? {
        val sensor = snapshotProvider.getFavoriteSensor(sensorId)
            ?: return emptySimpleResult(sensorId)
        val snapshot = snapshotProvider.getLatestSnapshot(sensor)
            ?: return emptySimpleResult(sensorId)
        val measurement = measurementFormatter.format(widgetType, snapshot)

        return SimpleWidgetData(
            sensorId = snapshot.sensorId,
            timestamp = snapshot.timestamp,
            displayName = snapshot.displayName,
            sensorValue = measurement.sensorValue,
            unit = measurement.unit,
            measurementName = context.getString(widgetType.titleResId),
            updated = snapshot.formattedTimestamp(),
        )
    }

    private fun WidgetSensorSnapshot.formattedTimestamp(): String {
        val measuredAt = timestamp
        return if (measuredAt.diffGreaterThan(hours24)) {
            measuredAt.localizedDate(context)
        } else {
            measuredAt.localizedTime(context)
        }
    }

    private val WidgetSensorSnapshot.timestamp: Date
        get() = Date(timestampEpochMillis)

    private fun emptySimpleResult(sensorId: String): SimpleWidgetData =
        SimpleWidgetData(
            sensorId = sensorId,
            timestamp = Date(0),
            displayName = context.getString(R.string.no_data),
            sensorValue = "",
            unit = "",
            measurementName = "",
            updated = null,
        )

    private fun emptyComplexResult(
        sensorId: String,
        displayName: String = context.getString(R.string.no_data),
    ): ComplexWidgetData =
        ComplexWidgetData(
            sensorId = sensorId,
            timestamp = Date(0),
            displayName = displayName,
            sensorValues = emptyList(),
            updated = null,
        )
}
