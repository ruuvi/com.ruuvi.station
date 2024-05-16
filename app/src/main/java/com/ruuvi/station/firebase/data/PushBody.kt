package com.ruuvi.station.firebase.data

import com.ruuvi.station.alarm.domain.AlarmType

data class GcmMessage(
    val GCM: PushBody
)

data class PushBody(
    val token: String,
    val email: String,
    val type: String,
    val data: AlertMessage
)

data class AlertMessage(
    val name: String,
    val id: String,
    val alertType: String,
    val triggerType: String,
    val currentValue: Double,
    val thresholdValue: Double,
    val alertUnit: String,
    val alertData: String,
    val showLocallyFormatted: Boolean?,
    val title: String,
    val body: String
) {

    val alarmType: AlarmType?
        get() = AlarmType.getByNetworkCode(alertType.lowercase())

}