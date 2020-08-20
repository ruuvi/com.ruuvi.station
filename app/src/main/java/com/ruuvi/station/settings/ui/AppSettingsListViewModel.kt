package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.model.HumidityUnit
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.util.BackgroundScanModes

class AppSettingsListViewModel(
    private val interactor: AppSettingsInteractor
) : ViewModel() {

    fun getBackgroundScanMode(): BackgroundScanModes =
        interactor.getBackgroundScanMode()

    fun isDashboardEnabled(): Boolean =
        interactor.isDashboardEnabled()

    fun setIsDashboardEnabled(isEnabled: Boolean) =
        interactor.setIsDashboardEnabled(isEnabled)

    fun getBackgroundScanInterval(): Int =
        interactor.getBackgroundScanInterval()

    fun getGatewayUrl(): String =
        interactor.getGatewayUrl()

    fun getTemperatureUnit(): String =
        interactor.getTemperatureUnit()

    fun getHumidityUnit(): HumidityUnit =
        interactor.getHumidityUnit()
}