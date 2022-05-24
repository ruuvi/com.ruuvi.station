package com.ruuvi.station.units.domain

import android.content.Context
import com.ruuvi.station.R

class AccelerationConverter (val context: Context) {

    fun getAccelerationString(acceleration: Double?, accelerationAxis: AccelerationAxis?): String {
        return if (acceleration == null) {
            UnitsConverter.NO_VALUE_AVAILABLE
        } else {
            context.getString(R.string.acceleration_reading, acceleration, getAccelerationUnit(accelerationAxis))
        }
    }

    fun getAccelerationStringWithoutUnit(acceleration: Double?): String {
        return if (acceleration == null) {
            UnitsConverter.NO_VALUE_AVAILABLE
        } else {
            context.getString(R.string.acceleration_reading, acceleration, "").trim()
        }
    }

    fun getAccelerationUnit(accelerationAxis: AccelerationAxis?): String =
        context.getString(accelerationAxis?.unitCode ?: R.string.acceleration_unit)
}