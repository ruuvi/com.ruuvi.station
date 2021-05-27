package com.ruuvi.station.network.data.request

import com.ruuvi.station.alarm.domain.AlarmElement
import com.ruuvi.station.alarm.domain.AlarmType

data class SetAlertRequest(
    val sensor: String,
    val type: String,
    val min: Int,
    val max: Int,
    val enabled: Boolean,
    val description: String
) {
    companion object {
        fun getAlarmRequest(alarm: AlarmElement): SetAlertRequest {
            val (low, high) =
                if (alarm.type == AlarmType.PRESSURE) {
                    Pair((alarm.low / 100), (alarm.high / 100))
                } else {
                    Pair(alarm.low, alarm.high)
                }
            return SetAlertRequest(
                sensor = alarm.sensorId,
                type = alarm.type.networkCode ?: throw IllegalArgumentException(),
                min = low,
                max = high,
                enabled = alarm.isEnabled,
                description = alarm.customDescription
            )
        }
    }
}