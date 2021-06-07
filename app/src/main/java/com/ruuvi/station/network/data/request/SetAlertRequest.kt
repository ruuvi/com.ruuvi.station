package com.ruuvi.station.network.data.request

import com.ruuvi.station.alarm.domain.AlarmElement

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
            return SetAlertRequest(
                sensor = alarm.sensorId,
                type = alarm.type.networkCode ?: throw IllegalArgumentException(),
                min = alarm.low,
                max = alarm.high,
                enabled = alarm.isEnabled,
                description = alarm.customDescription
            )
        }
    }
}