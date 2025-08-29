package com.ruuvi.station.units.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType

class MovementConverter(val context: Context) {
    fun getMovementString(movement: Int?): String {
        return if (movement != null) {
            "$movement ${getMovementUnit()}"
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

    fun getMovementUnit() = context.getText(R.string.movements)

    fun getMovementEnvironmentValue(
        movement: Int
    ): EnvironmentValue =
        EnvironmentValue (
            original = movement.toDouble(),
            value = movement.toDouble(),
            accuracy = Accuracy.Accuracy0,
            valueWithUnit = getMovementString(movement),
            valueWithoutUnit = getMovementStringWithoutUnit(movement),
            unitString = context.getString(UnitType.MovementUnit.MovementsCount.unit),
            unitType = UnitType.MovementUnit.MovementsCount
        )

    companion object {
        const val NO_VALUE_AVAILABLE = "-"
    }
}