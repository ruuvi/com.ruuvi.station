package com.ruuvi.station.network.domain

import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.network.data.response.NetworkAlertItem
import timber.log.Timber
import java.lang.Exception

class NetworkAlertsSyncInteractor(
    private val tokenRepository: NetworkTokenRepository,
    private val networkRepository: RuuviNetworkRepository,
    private val alarmRepository: AlarmRepository,
    private val sensorSettingsRepository: SensorSettingsRepository
) {
    private fun getToken() = tokenRepository.getTokenInfo()

    suspend fun updateAlertsFromNetwork() {
        try {
            getToken()?.token?.let { token ->
                val response = networkRepository.getAlerts(token = token, sensorId = null)
                if (response?.isSuccess() == true && response.data != null) {
                    response.data.sensors.forEach { sensor ->
                        if (sensorSettingsRepository.getSensorSettings(sensorId = sensor.sensor) != null) {
                            sensor.alerts.forEach { alert ->
                                saveNetworkAlert(sensor.sensor, alert)
                            }
                        }
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
            alarmRepository.upsertAlarm(
                sensorId = sensorId,
                min = alert.min,
                max = alert.max,
                enabled = alert.enabled,
                type = type.value,
                description = alert.description,
                mutedTill = null
            )
        } else {
            Timber.d("NetworkAlertsSyncInteractor-unknown alarm type: ${alert.type}")
        }
    }
}