package com.ruuvi.station.settings.domain

data class GatewayTestResult (
    val type: GatewayTestResultType,
    val code: Int? = null
)

enum class GatewayTestResultType {
    NONE,
    EXCEPTION,
    FAIL,
    SUCCESS,
    TESTING
}