package com.ruuvi.station.network.data.response

typealias GetSensorsResponse = RuuviNetworkResponse<GetSensorsResponseBody>

data class GetSensorsResponseBody(
    val sensors: List<SensorInfo>
)

data  class SensorInfo (
    val sensor: String,
    val name: String,
    val picture: String,
    val public: Boolean,
    val canShare: Boolean,
    val sharedTo: List<String>
)