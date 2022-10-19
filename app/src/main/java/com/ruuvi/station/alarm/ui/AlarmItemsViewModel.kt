package com.ruuvi.station.alarm.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.ruuvi.station.alarm.domain.AlarmItemState
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.alarm.domain.AlarmsInteractor
import com.ruuvi.station.util.extensions.diff
import com.ruuvi.station.util.extensions.equalsEpsilon
import com.ruuvi.station.util.extensions.round
import timber.log.Timber

class AlarmItemsViewModel(
    val sensorId: String,
    val alarmsInteractor: AlarmsInteractor,
): ViewModel() {

    private val _alarms = mutableStateListOf<AlarmItemState>(*alarmsInteractor.getAlarmsForSensor(sensorId).toTypedArray())
    val alarms: List<AlarmItemState> = _alarms

    var editLow: Boolean? = null
    var editHigh: Boolean? = null

    fun getPossibleRange(type: AlarmType): ClosedFloatingPointRange<Float> =
        alarmsInteractor.getPossibleRange(type)

    fun setEnabled(type: AlarmType, enabled: Boolean) {
        val alarmItem = _alarms.firstOrNull { it.type == type }

        if (alarmItem != null) {
            val newAlarm = alarmItem.copy(isEnabled = enabled, mutedTill = null)
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

    fun setRange(type: AlarmType, range: ClosedFloatingPointRange<Float>) {
        val alarmItem = _alarms.firstOrNull { it.type == type }
        val itemIndex = _alarms.indexOf(alarmItem)

        if (alarmItem != null) {
            Timber.d("setRange ${editLow}")
            val editLow = editLow ?: !range.start.equalsEpsilon(alarmItem.rangeLow, 0.01f)
            val editHigh = editHigh ?: !range.endInclusive.equalsEpsilon(alarmItem.rangeHigh, 0.01f)

            Timber.d("setRange $editLow  ${range.start} ${alarmItem.rangeLow} || ${range.endInclusive} ${alarmItem.rangeHigh}")
            if (editHigh || editLow) {
                val newAlarm = if (editHigh) {
                    val high = if (range.endInclusive.diff(alarmItem.rangeLow) < 1) {
                        (alarmItem.rangeLow + 1).round(0)
                    } else {
                        range.endInclusive
                    }
                    this.editHigh = editHigh
                    alarmItem.copy(
                        rangeHigh = high,
                        displayHigh = alarmsInteractor.getDisplayApproximateValue(high)
                    )
                } else {
                    val low = if (range.start.diff(alarmItem.rangeHigh) < 1) {
                        (alarmItem.rangeHigh - 1).round(0)
                    } else {
                        range.start
                    }
                    this.editLow = editLow
                    alarmItem.copy(
                        rangeLow = low,
                        displayLow =alarmsInteractor.getDisplayApproximateValue(low)
                    )
                }

                _alarms[itemIndex] = newAlarm
            }
        }
    }

    fun saveRange(type: AlarmType) {
        val alarmItem = _alarms.firstOrNull { it.type == type }
        val itemIndex = _alarms.indexOf(alarmItem)

        if (alarmItem != null) {

            if (editHigh == true) {
                var high = alarmItem.rangeHigh.round(0)

                val savableHigh = alarmsInteractor.getSavableValue(type, high)
                val newAlarm = alarmItem.copy(
                    max = savableHigh,
                    rangeHigh = alarmsInteractor.getRangeValue(type, savableHigh.toFloat()),
                    displayHigh = alarmsInteractor.getDisplayValue(high),
                )
                _alarms[itemIndex] = newAlarm
                saveAlarm(newAlarm)

            } else if (editLow == true) {
                var low = alarmItem.rangeLow.round(0)

                val savableLow = alarmsInteractor.getSavableValue(type, low)

                val newAlarm = alarmItem.copy(
                    min = savableLow,
                    rangeLow = alarmsInteractor.getRangeValue(type, savableLow.toFloat()),
                    displayLow = alarmsInteractor.getDisplayValue(low),
                )
                _alarms[itemIndex] = newAlarm
                saveAlarm(newAlarm)
            }

            editLow = null
            editHigh = null
        }
    }

    fun manualRangeSave(type: AlarmType, min: Double?, max: Double?) {
        val alarmItem = _alarms.firstOrNull { it.type == type }
        val itemIndex = _alarms.indexOf(alarmItem)

        if (validateRange(type, min, max) && alarmItem != null && min != null && max != null) {
            val savableHigh = alarmsInteractor.getSavableValue(type, max).round(2)
            val savableLow = alarmsInteractor.getSavableValue(type, min).round(2)

            val newAlarm = alarmItem.copy(
                min = savableLow,
                max = savableHigh,
                rangeLow = alarmsInteractor.getRangeValue(type, savableLow.toFloat()),
                displayLow = alarmsInteractor.getDisplayValue(min.toFloat()),
                rangeHigh = alarmsInteractor.getRangeValue(type, savableHigh.toFloat()),
                displayHigh = alarmsInteractor.getDisplayValue(max.toFloat()),
            )
            _alarms[itemIndex] = newAlarm
            saveAlarm(newAlarm)
        }
    }

    fun validateRange(type: AlarmType, min: Double?, max: Double?): Boolean {
        val possibleRange = getPossibleRange(type)
        val result = if (min != null && max !=null) {
            min >= possibleRange.start && max <= possibleRange.endInclusive && min < max
        } else {
            false
        }
        return result
    }

    fun getTitle(alarmType: AlarmType): String = alarmsInteractor.getAlarmTitle(alarmType)

    fun saveAlarm(alarmItemState: AlarmItemState) {
        alarmsInteractor.saveAlarm(alarmItemState)
    }

    fun refreshAlarmState() {
        val alertItems = alarmsInteractor.getAlarmsForSensor(sensorId)
        for ((index, item) in _alarms.withIndex()) {
            val updated = alertItems.firstOrNull { it.type == item.type }
            item.triggered = updated?.triggered ?: false
        }
    }
}