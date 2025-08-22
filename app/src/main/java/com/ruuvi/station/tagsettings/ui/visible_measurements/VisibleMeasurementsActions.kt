package com.ruuvi.station.tagsettings.ui.visible_measurements

sealed interface VisibleMeasurementsActions {
    data class ChangeUseDefault(val enabled: Boolean): VisibleMeasurementsActions
    data class AddToDisplayOrder(val unitCode: String): VisibleMeasurementsActions
    data class RemoveFromDisplayOrder(val unitCode: String): VisibleMeasurementsActions
    data class SwapDisplayOrderItems(val from: Int, val to: Int): VisibleMeasurementsActions
}