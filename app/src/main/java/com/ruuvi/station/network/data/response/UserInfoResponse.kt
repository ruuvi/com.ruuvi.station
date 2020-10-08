package com.ruuvi.station.network.data.response

typealias UserInfoResponse = RuuviNetworkResponse<UserInfoDataResponse>

data class UserInfoDataResponse (
    val email: String,
    val sensors: List<SensorDataResponse>
)

data class SensorDataResponse(
    val sensor: String,
    val name: String,
    val owner: Boolean,
    val picture: String,
    val public: Boolean
)