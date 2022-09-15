package com.ruuvi.station.alarm.domain

import com.ruuvi.station.database.tables.Alarm
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
) {
    fun getPossibleRange() = type.possibleRange

    companion object {
        fun getStateForDbAlarm(alarm: Alarm, alarmsInteractor: AlarmsInteractor): AlarmItemState {
            val rangeLow = alarmsInteractor.getRangeValue(alarm.alarmType, alarm.min.toFloat())
            val rangeHigh = alarmsInteractor.getRangeValue(alarm.alarmType, alarm.max.toFloat())
            return AlarmItemState(
                sensorId = alarm.ruuviTagId,
                type = alarm.alarmType,
                isEnabled = alarm.enabled,
                min = alarm.min,
                max = alarm.max,
                rangeLow = rangeLow,
                rangeHigh = rangeHigh,
                displayLow = alarmsInteractor.getDisplayValue(rangeLow),
                displayHigh = alarmsInteractor.getDisplayValue(rangeHigh),
                customDescription = alarm.customDescription,
                mutedTill = alarm.mutedTill
            )
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
