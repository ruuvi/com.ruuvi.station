package com.ruuvi.station.firebase.data

data class PushMessageRoot(
    val default: PushBody
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
    val alertData: String
)