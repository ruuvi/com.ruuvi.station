package com.ruuvi.station.network.data.response

typealias SensorDenseResponse = RuuviNetworkResponse<SensorsDenseResponseBody>

data class SensorsDenseResponseBody(
    val sensors: List<SensorsDenseInfo>
)

data class SensorsDenseInfo (
    val sensor: String,
    val owner: String,
    val name: String,
    val picture: String,
    val public: Boolean,
    val canShare: Boolean,
    val offsetTemperature: Double,
    val offsetHumidity: Double,
    val offsetPressure: Double,
    val measurements: List<SensorDataMeasurementResponse>
)