package com.ruuvi.station.network.data.response

typealias UserInfoResponse = RuuviNetworkResponse<UserInfoResponseBody>

data class UserInfoResponseBody (
    val email: String,
    val sensors: List<SensorDataResponse>
)

data class SensorDataResponse(
    val sensor: String,
    val name: String,
    val owner: String,
    val picture: String,
    val public: Boolean,
    val offsetTemperature: Double,
    val offsetHumidity: Double,
    val offsetPressure: Double
)