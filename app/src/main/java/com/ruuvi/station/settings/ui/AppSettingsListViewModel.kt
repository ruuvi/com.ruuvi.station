package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.feature.data.FeatureFlag
import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.units.model.UnitType.*
import com.ruuvi.station.util.BackgroundScanModes

class AppSettingsListViewModel(
    private val interactor: AppSettingsInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val runtimeBehavior: RuntimeBehavior
) : ViewModel() {

    val developerFeatures = preferencesRepository.getDeveloperSettingsLiveData()
    val temperatureUnit = interactor.getTemperatureUnitLiveData()
    val humidityUnit = interactor.getHumidityUnitLiveData()
    val pressureUnit = interactor.getPressureUnitLiveData()

    fun getBackgroundScanMode(): BackgroundScanModes =
        interactor.getBackgroundScanMode()

    fun getBackgroundScanInterval(): Int =
        interactor.getBackgroundScanInterval()

    fun getGatewayUrl(): String =
        interactor.getDataForwardingUrl()

    fun getTemperatureUnit(): TemperatureUnit =
        interactor.getTemperatureUnit()

    fun getHumidityUnit(): HumidityUnit =
        interactor.getHumidityUnit()

    fun getPressureUnit(): PressureUnit =
        interactor.getPressureUnit()

    fun isCloudModeEnabled(): Boolean =
        interactor.isCloudModeEnabled()

    fun setIsCloudModeEnabled(isEnabled: Boolean) =
        interactor.setIsCloudModeEnabled(isEnabled)

    fun shouldShowCloudMode() =
        interactor.shouldShowCloudMode()

    fun shouldShowGlobalUnitsSettings(): Boolean =
        runtimeBehavior.isFeatureEnabled(FeatureFlag.GLOBAL_UNITS_SETTINGS)
}
