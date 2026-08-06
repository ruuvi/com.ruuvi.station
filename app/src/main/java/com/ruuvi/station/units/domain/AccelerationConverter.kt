package com.ruuvi.station.units.domain

import android.content.Context
import com.ruuvi.station.R

class AccelerationConverter (val context: Context) {
    fun getAccelerationUnit(accelerationAxis: AccelerationAxis?): String =
        context.getString(accelerationAxis?.unitCode ?: R.string.acceleration_unit)
}