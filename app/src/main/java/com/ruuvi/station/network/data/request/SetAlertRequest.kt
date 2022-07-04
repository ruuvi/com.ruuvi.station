package com.ruuvi.station.network.data.request

import com.ruuvi.station.database.tables.Alarm

data class SetAlertRequest(
    val sensor: String,
    val type: String,
    val min: Int,
    val max: Int,
    val enabled: Boolean,
    val description: String
) {
    companion object {
        fun getAlarmRequest(alarm: Alarm): SetAlertRequest {
            return SetAlertRequest(
                sensor = alarm.ruuviTagId,
                type = alarm.alarmType?.networkCode ?: throw IllegalArgumentException(),
                min = alarm.low,
                max = alarm.high,
                enabled = alarm.enabled,
                description = alarm.customDescription
            )
        }
    }
}