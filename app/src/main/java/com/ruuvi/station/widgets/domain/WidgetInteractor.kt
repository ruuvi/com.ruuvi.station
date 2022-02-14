package com.ruuvi.station.widgets.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.extensions.diffGreaterThan
import com.ruuvi.station.util.extensions.hours24
import com.ruuvi.station.util.extensions.localizedDateTime
import com.ruuvi.station.util.extensions.localizedTime
import com.ruuvi.station.widgets.data.SimpleWidgetData
import com.ruuvi.station.widgets.data.WidgetData
import com.ruuvi.station.widgets.data.WidgetType

class WidgetInteractor (
    val tagRepository: TagRepository,
    val ruuviNetworkInteractor: NetworkDataSyncInteractor,
    val unitsConverter: UnitsConverter,
    val context: Context
) {
    suspend fun getSensorData(sensorId: String): WidgetData {
        var sensor = tagRepository.getFavoriteSensorById(sensorId)
        if (sensor == null || !canReturnData(sensor)) {
            return emptyResult(sensorId)
        }

        val syncJob = ruuviNetworkInteractor.syncNetworkData(true)
        syncJob.join()

        sensor = tagRepository.getFavoriteSensorById(sensorId)

        if (sensor != null) {
            return WidgetData(
                sensorId = sensorId,
                displayName = sensor.displayName,
                temperature = sensor.temperatureString,
                humidity = sensor.humidityString,
                pressure = sensor.pressureString,
                movement = sensor.movementCounter.toString(),
                updatedAt = sensor.updatedAt
            )
        } else {
            return emptyResult(sensorId)
        }
    }

    suspend fun getSimpleWidgetData(sensorId: String, widgetType: WidgetType): SimpleWidgetData {
        var sensorFav = tagRepository.getFavoriteSensorById(sensorId)
        if (sensorFav == null || !canReturnData(sensorFav)) {
            return emptySimpleResult(sensorId)
        }
        val syncJob = ruuviNetworkInteractor.syncNetworkData(true)
        syncJob.join()

        val sensorData =tagRepository.getTagById(sensorId)

        if (sensorData != null) {
            var unit = ""
            var sensorValue = ""
            when (widgetType) {
                WidgetType.TEMPERATURE -> {
                    unit = context.getString(unitsConverter.getTemperatureUnit().unit)
                    sensorValue = unitsConverter.getTemperatureStringWithoutUnit(sensorData.temperature)
                }
                WidgetType.HUMIDITY -> {
                    unit = context.getString(unitsConverter.getHumidityUnit().unit)
                    sensorValue = unitsConverter.getHumidityStringWithoutUnit(sensorData.humidity, sensorData.temperature ?: 0.0)
                }
                WidgetType.PRESSURE -> {
                    unit = context.getString(unitsConverter.getPressureUnit().unit)
                    sensorValue = unitsConverter.getPressureStringWithoutUnit(sensorData.pressure)
                }
                WidgetType.MOVEMENT -> {
                    unit = context.getString(R.string.movements)
                    sensorValue = sensorData.movementCounter.toString()
                }
                WidgetType.VOLTAGE -> {
                    unit = context.getString(R.string.voltage_unit)
                    sensorValue = context.getString(R.string.voltage_reading, sensorData.voltage.toString(), "").trim()
                }
                WidgetType.SIGNAL_STRENGTH -> {
                    unit = context.getString(R.string.signal_unit)
                    sensorValue = sensorData.rssi.toString()
                }
                WidgetType.ACCELERATION_X -> {
                    unit = context.getString(R.string.acceleration_unit)
                    sensorValue = String.format("%1\$,.3f", sensorData.accelX)
                }
                WidgetType.ACCELERATION_Y -> {
                    unit = context.getString(R.string.acceleration_unit)
                    sensorValue = String.format("%1\$,.3f", sensorData.accelY)
                }
                WidgetType.ACCELERATION_Z -> {
                    unit = context.getString(R.string.acceleration_unit)
                    sensorValue = String.format("%1\$,.3f", sensorData.accelZ)
                }
            }

            var updated: String? = null
            sensorData.updateAt?.let { updateAt ->
                updated = if (updateAt.diffGreaterThan(hours24)) {
                    updateAt.localizedDateTime(context)
                } else {
                    updateAt.localizedTime(context)
                }
            }

            return SimpleWidgetData(
                sensorId = sensorId,
                displayName = sensorFav.displayName,
                sensorValue = sensorValue,
                unit = unit,
                updated = updated
            )
        } else {
            return emptySimpleResult(sensorId)
        }

    }

    private fun emptyResult(sensorId: String): WidgetData = WidgetData(sensorId)

    private fun emptySimpleResult(sensorId: String): SimpleWidgetData = SimpleWidgetData(sensorId, sensorId, "", "", null)

    private fun canReturnData(sensor: RuuviTag) = sensor.networkLastSync != null
}