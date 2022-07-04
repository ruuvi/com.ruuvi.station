package com.ruuvi.station.units.domain

import android.content.Context
import com.ruuvi.station.R

class MovementConverter(val context: Context) {
    fun getMovementString(movement: Int?): String {
        return if (movement != null) {
            "$movement ${context.getText(R.string.movements)}"
        } else {
            NO_VALUE_AVAILABLE
        }
    }

    fun getMovementStringWithoutUnit (movement: Int?): String {
        return if (movement != null) {
            "$movement"
        } else {
            NO_VALUE_AVAILABLE
        }
    }

    companion object {
        const val NO_VALUE_AVAILABLE = "-"
    }
}