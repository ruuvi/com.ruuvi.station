package com.ruuvi.station.network.data.response

typealias GetSensorDataResponse = RuuviNetworkResponse<GetSensorDataDataResponse>

data class GetSensorDataDataResponse (
    val sensor: String,
    val total: Int,
    val measurements: List<SensorDataMeasurementResponse>
)

data class SensorDataMeasurementResponse (
    val sensor: String,
    val coordinates: String,
    val gwmac: String,
    val data: String,
    val timestamp: Long,
    val rssi: Int
)