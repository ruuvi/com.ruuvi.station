package com.ruuvi.station.alarm.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.alarm.domain.AlarmsInteractor

class AlarmItemsViewModel(
    val sensorId: String,
    val alarmsInteractor: AlarmsInteractor
): ViewModel() {
    fun getAvailableAlarmTypesForSensor() =
        alarmsInteractor.getAvailableAlarmTypesForSensor(sensorId)

    fun getAlarmsForSensor() =
        alarmsInteractor.getAlarmsForSensor(sensorId)
}