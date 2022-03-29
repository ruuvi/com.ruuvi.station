package com.ruuvi.station.units.domain

class MovementConverter() {
    fun getMovementString(movement: Int?): String {
        return movement?.toString() ?: NO_VALUE_AVAILABLE
    }

    companion object {
        const val NO_VALUE_AVAILABLE = "-"
    }
}