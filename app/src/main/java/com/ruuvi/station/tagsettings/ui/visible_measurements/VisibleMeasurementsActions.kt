package com.ruuvi.station.tagsettings.ui.visible_measurements

sealed interface VisibleMeasurementsActions {
    data class ChangeUseDefault(val enabled: Boolean): VisibleMeasurementsActions
    data class AddToDisplayOrder(val unitCode: String): VisibleMeasurementsActions
    data class RemoveFromDisplayOrder(val unitCode: String): VisibleMeasurementsActions
    data class RemoveFromDisplayOrderAndDisableAlert(val unitCode: String): VisibleMeasurementsActions
    data class SwapDisplayOrderItems(val from: Int, val to: Int): VisibleMeasurementsActions
    data class ChangeUseDefaultAndDisableAlert(val unitCode: String): VisibleMeasurementsActions
}

sealed interface VisibleMeasurementsEffect {
    data class AskRemovalConfirmation(
        val unitCode: String,
    ) : VisibleMeasurementsEffect

    data class AskChangeUseDefaultConfirmation(
        val unitCode: List<String>,
    ) : VisibleMeasurementsEffect


    object ForbiddenRemoveLast : VisibleMeasurementsEffect
}