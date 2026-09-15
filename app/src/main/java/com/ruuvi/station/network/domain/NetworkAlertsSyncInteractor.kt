package com.ruuvi.station.network.domain

import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.network.data.response.NetworkAlertItem
import com.ruuvi.station.network.data.response.SensorDenseResponse
import timber.log.Timber
import java.lang.Exception

class NetworkAlertsSyncInteractor(
    private val alarmRepository: AlarmRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val networkInteractor: RuuviNetworkInteractor
) {

    fun updateAlertsFromNetwork(sensors: SensorDenseResponse) {
        try {
            sensors.data?.sensors?.forEach { sensor ->
                if (sensorSettingsRepository.getSensorSettings(sensorId = sensor.sensor) != null) {
                    val localAlerts = alarmRepository.getForSensor(sensor.sensor)
                    val networkAlertsByType = sensor.alerts.associateBy { it.type }

                    localAlerts.forEach { localAlert ->
                        val networkCode = localAlert.alarmType.networkCode ?: return@forEach
                        val networkAlert = networkAlertsByType[networkCode]

                        if (networkAlert == null || localAlert.lastUpdated > networkAlert.lastUpdated) {
                            networkInteractor.setAlert(localAlert)
                        }
                    }

                    sensor.alerts.forEach { alert ->
                        saveNetworkAlert(sensor.sensor, alert, localAlerts)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "NetworkAlarmsSyncInteractor-updateAlarmsFromNetwork")
        }
    }

    private fun saveNetworkAlert(sensorId: String, alert: NetworkAlertItem, localAlerts: List<Alarm>) {
        val type = AlarmType.getByNetworkCode(alert.type)
        if (type != null) {
            val savedAlert = localAlerts.firstOrNull { it.type == type.value }
            if (alert.lastUpdated > (savedAlert?.lastUpdated ?: 0)) {
                alarmRepository.upsertAlarm(
                    sensorId = sensorId,
                    min = alert.min,
                    max = alert.max,
                    enabled = alert.enabled,
                    type = type.value,
                    description = alert.description,
                    mutedTill = savedAlert?.mutedTill,
                    timestamp = alert.lastUpdated
                )
            }
        } else {
            Timber.d("NetworkAlertsSyncInteractor-unknown alarm type: ${alert.type}")
        }
    }
}