package com.ruuvi.station.calibration.model

import com.ruuvi.station.R

enum class CalibrationType (val titleId: Int) {
    TEMPERATURE (R.string.calibration_temperature_offset),
    HUMIDITY (R.string.calibration_humidity_offset),
    PRESSURE (R.string.calibration_pressure_offset)
}