package com.ruuvi.station.widgets.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.extensions.*
import com.ruuvi.station.widgets.data.SimpleWidgetData
import com.ruuvi.station.widgets.data.WidgetData
import com.ruuvi.station.widgets.data.WidgetType
import timber.log.Timber
import java.util.*

class WidgetInteractor (
    val context: Context,
    val tagRepository: TagRepository,
    val syncInteractor: NetworkDataSyncInteractor,
    val cloudInteractor: RuuviNetworkInteractor,
    val unitsConverter: UnitsConverter
) {
    suspend fun getSensorData(sensorId: String): WidgetData {
        var sensor = tagRepository.getFavoriteSensorById(sensorId)
        if (sensor == null || !canReturnData(sensor)) {
            return emptyResult(sensorId)
        }

        val syncJob = syncInteractor.syncNetworkData(true)
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

        val lastData = cloudInteractor.getSensorLastData(sensorId)

        Timber.d(lastData.toString())

        if (lastData?.isSuccess() == true && lastData.data?.measurements?.isNotEmpty() == true) {
            val measurement = lastData.data.measurements.first()
            val decoded = BluetoothLibrary.decode(sensorId, measurement.data, measurement.rssi)
            decoded.temperature?.let { temperature ->
                decoded.temperature = temperature + (lastData.data.offsetTemperature ?: 0.0)
            }
            decoded.humidity?.let { humidity ->
                decoded.humidity = humidity + (lastData.data.offsetHumidity ?: 0.0)
            }
            decoded.pressure?.let { pressure ->
                decoded.pressure = pressure + (lastData.data.offsetPressure ?: 0.0)
            }

            val updatedDate = Date(measurement.timestamp * 1000)

            var unit = ""
            var sensorValue = ""
            when (widgetType) {
                WidgetType.TEMPERATURE -> {
                    unit = context.getString(unitsConverter.getTemperatureUnit().unit)
                    sensorValue =
                        unitsConverter.getTemperatureStringWithoutUnit(decoded.temperature)
                }
                WidgetType.HUMIDITY -> {
                    unit = context.getString(unitsConverter.getHumidityUnit().unit)
                    sensorValue = unitsConverter.getHumidityStringWithoutUnit(
                        decoded.humidity,
                        decoded.temperature ?: 0.0
                    )
                }
                WidgetType.PRESSURE -> {
                    unit = context.getString(unitsConverter.getPressureUnit().unit)
                    sensorValue = unitsConverter.getPressureStringWithoutUnit(decoded.pressure)
                }
                WidgetType.MOVEMENT -> {
                    unit = context.getString(R.string.movements)
                    sensorValue = decoded.movementCounter.toString()
                }
                WidgetType.VOLTAGE -> {
                    unit = context.getString(R.string.voltage_unit)
                    sensorValue =
                        context.getString(R.string.voltage_reading, decoded.voltage.toString(), "")
                            .trim()
                }
                WidgetType.SIGNAL_STRENGTH -> {
                    unit = context.getString(R.string.signal_unit)
                    sensorValue = decoded.rssi.toString()
                }
                WidgetType.ACCELERATION_X -> {
                    unit = context.getString(R.string.acceleration_unit)
                    sensorValue = String.format("%1\$,.3f", decoded.accelX)
                }
                WidgetType.ACCELERATION_Y -> {
                    unit = context.getString(R.string.acceleration_unit)
                    sensorValue = String.format("%1\$,.3f", decoded.accelY)
                }
                WidgetType.ACCELERATION_Z -> {
                    unit = context.getString(R.string.acceleration_unit)
                    sensorValue = String.format("%1\$,.3f", decoded.accelZ)
                }
            }

            val updated = if (updatedDate.diffGreaterThan(hours24)) {
                updatedDate.localizedDate(context)
            } else {
                updatedDate.localizedTime(context)
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