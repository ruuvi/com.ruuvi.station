package com.ruuvi.station.units.model

data class EnvironmentValue(
    val value: Double,
    val accuracy: Accuracy,
    val valueWithUnit: String,
    val valueWithoutUnit: String,
    val unitString: String
)