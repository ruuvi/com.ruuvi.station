package com.ruuvi.station.tagsettings.ui.visible_measurements

import com.ruuvi.station.units.model.UnitType

sealed interface VisibleMeasurementsActions {
    data class ChangeUseDefault(val useDefault: Boolean): VisibleMeasurementsActions
    data class AddToDisplayOrder(val unit: UnitType): VisibleMeasurementsActions
    data class RemoveFromDisplayOrder(val unit: UnitType): VisibleMeasurementsActions
    data class RemoveFromDisplayOrderAndDisableAlert(val unit: UnitType): VisibleMeasurementsActions
    data class SwapDisplayOrderItems(val from: Int, val to: Int): VisibleMeasurementsActions
    data class ChangeUseDefaultAndDisableAlert(val useDefault: Boolean, val units: List<UnitType>): VisibleMeasurementsActions
}

sealed interface VisibleMeasurementsEffect {
    data class AskRemovalConfirmation(
        val unit: UnitType,
    ) : VisibleMeasurementsEffect

    data class AskChangeUseDefaultConfirmation(
        val useDefault: Boolean,
        val units: List<UnitType>,
    ) : VisibleMeasurementsEffect


    object ForbiddenRemoveLast : VisibleMeasurementsEffect
}