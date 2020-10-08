package com.ruuvi.station.network.data.response

typealias UserVerifyResponse = RuuviNetworkResponse<UserVerifyDataResponse>

data class UserVerifyDataResponse (
    val email: String,
    val accessToken: String,
    val newUser: Boolean
)