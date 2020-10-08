package com.ruuvi.station.network.data.response

typealias UserRegisterResponse = RuuviNetworkResponse<UserRegisterDataResponse>

data class UserRegisterDataResponse (
    val email: String
)