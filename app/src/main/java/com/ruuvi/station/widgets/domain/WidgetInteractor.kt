package com.ruuvi.station.widgets.domain

import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.widgets.data.WidgetData

class WidgetInteractor (
    val tagRepository: TagRepository,
    val ruuviNetworkInteractor: NetworkDataSyncInteractor
) {
    suspend fun getSensorData(sensorId: String): WidgetData {
        var sensor = tagRepository.getFavoriteSensorById(sensorId)
        if (sensor == null || !canReturnData(sensor)) {
            return emptyResult(sensorId)
        }

        val syncJob = ruuviNetworkInteractor.syncNetworkData()
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

    private fun emptyResult(sensorId: String): WidgetData = WidgetData(sensorId)

    private fun canReturnData(sensor: RuuviTag) = sensor.networkLastSync != null
}