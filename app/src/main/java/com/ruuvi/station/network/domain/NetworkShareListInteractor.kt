package com.ruuvi.station.network.domain

import com.ruuvi.station.database.domain.SensorShareListRepository
import com.ruuvi.station.network.data.response.SensorDenseResponse
import timber.log.Timber

class NetworkShareListInteractor(
    val sensorShareListRepository: SensorShareListRepository
) {
    fun updateSharingInfo(sensorsInfo: SensorDenseResponse) {
        try {
            if (sensorsInfo.data?.sensors != null) {
                for (sensor in sensorsInfo.data.sensors) {
                    sensorShareListRepository.updateSharingList(sensor.sensor, sensor.sharedTo)
                }
            }
        } catch (e: Exception) {
            Timber.e(e,"updateSharingInfo Exception")
        }
    }
}