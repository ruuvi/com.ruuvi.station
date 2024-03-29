package com.ruuvi.station.network.data.response

data class RuuviNetworkResponse<T> (
    val result: String,
    var error: String,
    val data: T?,
    val code: String?
) {
    fun isSuccess() = result == successResult

    fun isError() = result == errorResult

    companion object{
        const val successResult = "success"
        const val errorResult = "error"
    }
}