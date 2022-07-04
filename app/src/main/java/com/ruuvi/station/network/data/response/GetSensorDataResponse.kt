package com.ruuvi.station.network.data.response

typealias GetSensorDataResponse = RuuviNetworkResponse<GetSensorDataResponseBody>

data class GetSensorDataResponseBody (
    val sensor: String,
    val offsetTemperature: Double,
    val offsetHumidity: Double,
    val offsetPressure: Double,
    val total: Int,
    val measurements: List<SensorDataMeasurementResponse>
)

data class SensorDataMeasurementResponse (
    val coordinates: String,
    val gwmac: String,
    val data: String,
    val timestamp: Long,
    val rssi: Int
)