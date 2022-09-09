package com.ruuvi.station.alarm.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.ruuvi.station.alarm.domain.AlarmItemState
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.alarm.domain.AlarmsInteractor

class AlarmItemsViewModel(
    val sensorId: String,
    val alarmsInteractor: AlarmsInteractor
): ViewModel() {

    private val _alarms = mutableStateListOf<AlarmItemState>(*alarmsInteractor.getAlarmsForSensor(sensorId).toTypedArray())
    val alarms: List<AlarmItemState> = _alarms

    fun setEnabled(type: AlarmType, enabled: Boolean) {
        val alarmItem = _alarms.firstOrNull { it.type == type }

        if (alarmItem != null) {
            val newAlarm = alarmItem.copy(isEnabled = enabled)
            _alarms[_alarms.indexOf(alarmItem)] = newAlarm
            saveAlarm(newAlarm)
        }
    }

    fun setDescription(type: AlarmType, description: String) {
        val alarmItem = _alarms.firstOrNull { it.type == type }

        if (alarmItem != null) {
            val newAlarm = alarmItem.copy(customDescription = description)
            _alarms[_alarms.indexOf(alarmItem)] = newAlarm
            saveAlarm(newAlarm)
        }
    }

    fun setRange(type: AlarmType, range: IntRange) {
        val alarmItem = _alarms.firstOrNull { it.type == type }

        if (alarmItem != null) {
            val newAlarm = alarmItem.copy(low = range.first, high = range.last)
            _alarms[_alarms.indexOf(alarmItem)] = alarmItem.copy(low = range.first, high = range.last)
        }
    }

    fun saveRange(type: AlarmType) {
        val alarmItem = _alarms.firstOrNull { it.type == type }

        if (alarmItem != null) {
            saveAlarm(alarmItem)
        }
    }

    fun getTitle(alarmType: AlarmType): String = alarmsInteractor.getAlarmTitle(alarmType)

    fun getDisplayValue(alarmType: AlarmType, value: Int): Int = alarmsInteractor.getDisplayValue(alarmType, value)

    fun saveAlarm(alarmItemState: AlarmItemState) {
        alarmsInteractor.saveAlarm(alarmItemState)
    }
}