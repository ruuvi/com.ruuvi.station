package com.ruuvi.station.network.data.response

typealias SensorDenseResponse = RuuviNetworkResponse<SensorsDenseResponseBody>

data class SensorsDenseResponseBody(
    val sensors: List<SensorsDenseInfo>
)

data class SensorsDenseInfo (
    val sensor: String,
    val owner: String,
    val name: String,
    val picture: String,
    val public: Boolean,
    val canShare: Boolean,
    val offsetTemperature: Double,
    val offsetHumidity: Double,
    val offsetPressure: Double,
    val measurements: List<SensorDataMeasurementResponse>,
    val alerts: List<NetworkAlertItem>,
    val sharedTo: List<String>,
    val subscription: SensorSubscription
)

data class SensorSubscription(
    val maxHistoryDays: Int,
    val maxResolutionMinutes: Int,
    val emailAlertAllowed: Boolean,
    val pushAlertAllowed: Boolean,
    val subscriptionName: String
)