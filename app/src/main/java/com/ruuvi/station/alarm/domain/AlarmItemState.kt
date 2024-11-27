package com.ruuvi.station.alarm.domain

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.ruuvi.station.database.tables.Alarm
import timber.log.Timber
import java.util.*

data class AlarmItemState(
    val sensorId: String,
    var type: AlarmType,
    var isEnabled: MutableState<Boolean>,
    var min: Double,
    var max: Double,
    var rangeLow: Float,
    var rangeHigh: Float,
    var displayLow: String,
    var displayHigh: String,
    var customDescription: String = "",
    var mutedTill: Date? = null,
    var triggered: Boolean = false,
    var extended: Boolean = false
) {
    companion object {
        fun getStateForDbAlarm(alarm: Alarm, alarmsInteractor: AlarmsInteractor): AlarmItemState {
            val type = alarm.alarmType
            val alarmMin = minOf(alarm.min, alarm.max)
            val alarmMax = maxOf(alarm.min, alarm.max)
            var min = if (type.valueInRange(alarmMin)) alarmMin else type.possibleRange.first
            var max = if (type.valueInRange(alarmMin)) alarmMax else min
            val rangeLow = alarmsInteractor.getRangeValue(type, min.toFloat())
            val rangeHigh = alarmsInteractor.getRangeValue(type, max.toFloat())

            if (type == AlarmType.OFFLINE) {
                min = 0
                max = if (type.valueInRange(alarm.max)) alarm.max else 15 * 60
            }

            val state = AlarmItemState(
                sensorId = alarm.ruuviTagId,
                type = type,
                isEnabled = mutableStateOf(alarm.enabled),
                min = min.toDouble(),
                max = max.toDouble(),
                rangeLow = rangeLow,
                rangeHigh = rangeHigh,
                displayLow = alarmsInteractor.getDisplayValue(rangeLow),
                displayHigh = alarmsInteractor.getDisplayValue(rangeHigh),
                customDescription = alarm.customDescription,
                mutedTill = alarm.mutedTill,
                extended = alarm.extended
            )
            Timber.d("getStateForDbAlarm $alarm \n $state")
            return state
        }

        fun getDefaultState(sensorId: String, alarmType: AlarmType, alarmsInteractor: AlarmsInteractor): AlarmItemState {
            val rangeLow = alarmsInteractor.getRangeValue(alarmType, alarmType.possibleRange.first.toFloat())
            val rangeHigh = alarmsInteractor.getRangeValue(alarmType, alarmType.possibleRange.last.toFloat())
            val min = if (alarmType == AlarmType.OFFLINE) 0 else alarmType.possibleRange.first
            val max = if (alarmType == AlarmType.OFFLINE) 15 * 60 else alarmType.possibleRange.last
            return AlarmItemState(
                sensorId = sensorId,
                type = alarmType,
                isEnabled = mutableStateOf(false),
                min = min.toDouble(),
                max = max.toDouble(),
                rangeLow = rangeLow,
                rangeHigh = rangeHigh,
                displayLow = alarmsInteractor.getDisplayValue(rangeLow),
                displayHigh = alarmsInteractor.getDisplayValue(rangeHigh),
            )
        }
    }
}
