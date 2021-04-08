package com.ruuvi.station.calibration.model

enum class CalibrationType (val title: String) {
    TEMPERATURE ("Temperature offset"),
    HUMIDITY ("Humidity offset"),
    PRESSURE ("Pressure offset")
}