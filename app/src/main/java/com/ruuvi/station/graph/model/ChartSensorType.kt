package com.ruuvi.station.graph.model

import com.ruuvi.station.R

enum class ChartSensorType (val captionTemplate: Int) {
    TEMPERATURE (R.string.temperature_with_unit),
    HUMIDITY (R.string.humidity_with_unit),
    PRESSURE (R.string.pressure_with_unit)
}