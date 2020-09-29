package com.ruuvi.station.network.data

data class UserRegisterResponse (
    val result: String,
    val data: UserRegisterDataResponse,
    val error: String
)

data class UserRegisterDataResponse (
    val email:String
)
