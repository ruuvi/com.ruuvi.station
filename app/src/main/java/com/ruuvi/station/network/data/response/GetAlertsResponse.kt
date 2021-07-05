package com.ruuvi.station.network.data.response

typealias GetAlertsResponse = RuuviNetworkResponse<GetAlertsResponseBody>

data class GetAlertsResponseBody(
    val sensors: List<NetworkSensorItem>
)

data  class NetworkSensorItem (
    val sensor: String,
    val alerts: List<NetworkAlertItem>
)

data class NetworkAlertItem(
    val type: String,
    val min: Double,
    val max: Double,
    val enabled: Boolean,
    val description: String
)