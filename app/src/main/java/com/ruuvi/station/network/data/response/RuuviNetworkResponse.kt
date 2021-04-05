package com.ruuvi.station.network.data.response

data class RuuviNetworkResponse<T> (
    val result: String,
    val error: String,
    val data: T?
) {
    fun isSuccess() = result == successResult

    fun isError() = result == errorResult

    companion object{
        const val successResult = "success"
        const val errorResult = "error"
    }
}