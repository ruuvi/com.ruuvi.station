package com.ruuvi.station.network.data.response

typealias SharedSensorsResponse = RuuviNetworkResponse<SharedSensorsResponseBody>

data class SharedSensorsResponseBody (
    val sensors: List<SharedSensorDataResponse>
)

data class SharedSensorDataResponse(
    val sensor: String,
    val name: String,
    val picture: String,
    val public: Boolean,
    val sharedTo: String
)