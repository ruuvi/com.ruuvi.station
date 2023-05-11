package com.ruuvi.station.alarm.domain

import com.ruuvi.station.database.tables.Alarm
import timber.log.Timber
import java.util.*

data class AlarmItemState(
    val sensorId: String,
    var type: AlarmType,
    var isEnabled: Boolean,
    var min: Double,
    var max: Double,
    var rangeLow: Float,
    var rangeHigh: Float,
    var displayLow: String,
    var displayHigh: String,
    var customDescription: String = "",
    var mutedTill: Date? = null,
    var triggered: Boolean = false
) {
    companion object {
        fun getStateForDbAlarm(alarm: Alarm, alarmsInteractor: AlarmsInteractor): AlarmItemState {
            val type = alarm.alarmType
            val alarmMin = minOf(alarm.min, alarm.max)
            val alarmMax = maxOf(alarm.min, alarm.max)
            val min = if (type.valueInRange(alarmMin)) alarmMin else type.possibleRange.first
            val max = if (type.valueInRange(alarmMin)) alarmMax else min
            val rangeLow = alarmsInteractor.getRangeValue(type, min.toFloat())
            val rangeHigh = alarmsInteractor.getRangeValue(type, max.toFloat())

            val state = AlarmItemState(
                sensorId = alarm.ruuviTagId,
                type = type,
                isEnabled = alarm.enabled,
                min = min.toDouble(),
                max = max.toDouble(),
                rangeLow = rangeLow,
                rangeHigh = rangeHigh,
                displayLow = alarmsInteractor.getDisplayValue(rangeLow),
                displayHigh = alarmsInteractor.getDisplayValue(rangeHigh),
                customDescription = alarm.customDescription,
                mutedTill = alarm.mutedTill
            )
            Timber.d("getStateForDbAlarm $alarm \n $state")
            return state
        }

        fun getDefaultState(sensorId: String, alarmType: AlarmType, alarmsInteractor: AlarmsInteractor): AlarmItemState {
            val rangeLow = alarmsInteractor.getRangeValue(alarmType, alarmType.possibleRange.first.toFloat())
            val rangeHigh = alarmsInteractor.getRangeValue(alarmType, alarmType.possibleRange.last.toFloat())
            return AlarmItemState(
                sensorId = sensorId,
                type = alarmType,
                isEnabled = false,
                min = alarmType.possibleRange.first.toDouble(),
                max = alarmType.possibleRange.last.toDouble(),
                rangeLow = rangeLow,
                rangeHigh = rangeHigh,
                displayLow = alarmsInteractor.getDisplayValue(rangeLow),
                displayHigh = alarmsInteractor.getDisplayValue(rangeHigh),
            )
        }
    }
}
