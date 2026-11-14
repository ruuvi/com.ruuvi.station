package com.ruuvi.station.network.data.response

import com.google.gson.annotations.SerializedName

data class RuuviNetworkResponse<T> (
    val result: String,
    var error: String,
    val data: T?,
    val code: String?,
    @SerializedName("sub_code") val subCode: String? = null
) {
    fun isSuccess() = result == successResult

    fun isError() = result == errorResult

    companion object{
        const val successResult = "success"
        const val errorResult = "error"
    }
}