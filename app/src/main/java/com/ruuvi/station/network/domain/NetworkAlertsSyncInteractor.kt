package com.ruuvi.station.network.domain

import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.network.data.response.NetworkAlertItem
import com.ruuvi.station.network.data.response.SensorDenseResponse
import timber.log.Timber
import java.lang.Exception

class NetworkAlertsSyncInteractor(
    private val alarmRepository: AlarmRepository,
    private val sensorSettingsRepository: SensorSettingsRepository
) {

    fun updateAlertsFromNetwork(sensors: SensorDenseResponse) {
        try {
            sensors.data?.sensors?.forEach { sensor ->
                if (sensorSettingsRepository.getSensorSettings(sensorId = sensor.sensor) != null) {
                    sensor.alerts.forEach { alert ->
                        saveNetworkAlert(sensor.sensor, alert)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "NetworkAlarmsSyncInteractor-updateAlarmsFromNetwork")
        }
    }

    private fun saveNetworkAlert(sensorId: String, alert: NetworkAlertItem) {
        val type = AlarmType.getByNetworkCode(alert.type)
        if (type != null) {
            val savedAlert = alarmRepository.getForSensor(sensorId).firstOrNull { it.type == type.value }
            alarmRepository.upsertAlarm(
                sensorId = sensorId,
                min = alert.min,
                max = alert.max,
                enabled = alert.enabled,
                type = type.value,
                description = alert.description,
                mutedTill = savedAlert?.mutedTill
            )
        } else {
            Timber.d("NetworkAlertsSyncInteractor-unknown alarm type: ${alert.type}")
        }
    }
}