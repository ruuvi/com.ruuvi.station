package com.ruuvi.station.network.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.data.response.NetworkUserSettings
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.units.model.TemperatureUnit
import com.ruuvi.station.util.BackgroundScanModes
import timber.log.Timber
import java.lang.Exception

class NetworkApplicationSettings (
    private val tokenRepository: NetworkTokenRepository,
    private val networkRepository: RuuviNetworkRepository,
    private val networkInteractor: RuuviNetworkInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val unitsConverter: UnitsConverter
    ) {

    private fun getToken() = tokenRepository.getTokenInfo()

    suspend fun updateSettingsFromNetwork() {
        try {
            getToken()?.token?.let { token ->
                val response = networkRepository.getUserSettings(token)
                if (response?.data != null && response.isSuccess()) {
                    if (initializeSettings(response.data.settings)) {
                        applyBackgroundScanMode(response.data.settings)
                        applyBackgroundScanInterval(response.data.settings)
                        applyTemperatureUnit(response.data.settings)
                        applyHumidityUnit(response.data.settings)
                        applyPressureUnit(response.data.settings)
                        applyDashboardEnabled(response.data.settings)
                        applyChartShowAllPoints(response.data.settings)
                        applyChartDrawDots(response.data.settings)
                        applyChartViewPeriod(response.data.settings)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "NetworkApplicationSettings-updateSettingsFromNetwork")
        }
    }

    private fun initializeSettings(settings: NetworkUserSettings): Boolean {
        return if (settings.isEmpty()) {
            Timber.d("NetworkApplicationSettings-initializeSettings")
            updateBackgroundScanMode()
            updateBackgroundScanInterval()
            updateTemperatureUnit()
            updateHumidityUnit()
            updatePressureUnit()
            updateDashboardEnabled()
            updateChartShowAllPoints()
            updateChartDrawDots()
            updateChartViewPeriod()
            false
        } else {
            true
        }
    }

    private fun applyBackgroundScanMode(settings: NetworkUserSettings) {
        val mode = settings.BACKGROUND_SCAN_MODE?.toIntOrNull()
        if (mode != null) {
            BackgroundScanModes.fromInt(mode)?.let {
                Timber.d("NetworkApplicationSettings-applyBackgroundScanMode: $it")
                preferencesRepository.setBackgroundScanMode(it)
            }
        }
    }

    private fun applyBackgroundScanInterval(settings: NetworkUserSettings) {
        settings.BACKGROUND_SCAN_INTERVAL?.toIntOrNull()?.let {
            Timber.d("NetworkApplicationSettings-applyBackgroundScanInterval: $it")
            preferencesRepository.setBackgroundScanInterval(it)
        }
    }

    private fun applyTemperatureUnit(settings: NetworkUserSettings) {
        settings.UNIT_TEMPERATURE?.let {
            val unit = TemperatureUnit.getByCode(it)
            if (unit != null) {
                Timber.d("NetworkApplicationSettings-applyTemperatureUnit: $unit")
                preferencesRepository.setTemperatureUnit(unit)
            }
        }
    }

    private fun applyHumidityUnit(settings: NetworkUserSettings) {
        settings.UNIT_HUMIDITY?.toIntOrNull()?.let {
            val unit = HumidityUnit.getByCode(it)
            if (unit != null) {
                Timber.d("NetworkApplicationSettings-applyHumidityUnit: $unit")
                preferencesRepository.setHumidityUnit(unit)
            }
        }
    }

    private fun applyPressureUnit(settings: NetworkUserSettings) {
        settings.UNIT_PRESSURE?.toIntOrNull()?.let {
            val unit = PressureUnit.getByCode(it)
            if (unit != null) {
                Timber.d("NetworkApplicationSettings-applyPressureUnit: $unit")
                preferencesRepository.setPressureUnit(unit)
            }
        }
    }

    private fun applyDashboardEnabled(settings: NetworkUserSettings) {
        if (settings.DASHBOARD_ENABLED != null) {
            settings.DASHBOARD_ENABLED.toBoolean().let {
                Timber.d("NetworkApplicationSettings-applyDashboardEnabled: $it")
                preferencesRepository.setIsDashboardEnabled(it)
            }
        }
    }

    private fun applyChartShowAllPoints(settings: NetworkUserSettings) {
        if (settings.CHART_SHOW_ALL_POINTS != null) {
            settings.CHART_SHOW_ALL_POINTS.toBoolean().let {
                Timber.d("NetworkApplicationSettings-applyChartShowAllPoints: $it")
                preferencesRepository.setIsShowAllGraphPoint(it)
            }
        }
    }

    private fun applyChartDrawDots(settings: NetworkUserSettings) {
        if (settings.CHART_DRAW_DOTS != null) {
            settings.CHART_DRAW_DOTS.toBoolean().let {
                Timber.d("NetworkApplicationSettings-applyChartDrawDots: $it")
                preferencesRepository.setGraphDrawDots(it)
            }
        }
    }

    private fun applyChartViewPeriod(settings: NetworkUserSettings) {
        settings.CHART_VIEW_PERIOD?.toIntOrNull()?.let {
            Timber.d("NetworkApplicationSettings-applyChartViewPeriod: $it")
            preferencesRepository.setGraphViewPeriodDays(it)
        }
    }

    fun updateBackgroundScanMode() {
        if (networkInteractor.signedIn) {
            networkInteractor.updateUserSetting(
                BACKGROUND_SCAN_MODE,
                preferencesRepository.getBackgroundScanMode().value.toString()
            )
        }
    }

    fun updateTemperatureUnit() {
        if (networkInteractor.signedIn) {
            networkInteractor.updateUserSetting(
                UNIT_TEMPERATURE,
                unitsConverter.getTemperatureUnit().code
            )
        }
    }

    fun updateHumidityUnit() {
        if (networkInteractor.signedIn) {
            networkInteractor.updateUserSetting(
                UNIT_HUMIDITY,
                unitsConverter.getHumidityUnit().code.toString()
            )
        }
    }

    fun updateDashboardEnabled() {
        if (networkInteractor.signedIn) {
            networkInteractor.updateUserSetting(
                DASHBOARD_ENABLED,
                preferencesRepository.isDashboardEnabled().toString()
            )
        }
    }

    fun updateBackgroundScanInterval() {
        if (networkInteractor.signedIn) {
            networkInteractor.updateUserSetting(
                BACKGROUND_SCAN_INTERVAL,
                preferencesRepository.getBackgroundScanInterval().toString()
            )
        }
    }

    fun updateChartShowAllPoints() {
        if (networkInteractor.signedIn) {
            networkInteractor.updateUserSetting(
                CHART_SHOW_ALL_POINTS,
                preferencesRepository.isShowAllGraphPoint().toString()
            )
        }
    }

    fun updateChartDrawDots() {
        if (networkInteractor.signedIn) {
            networkInteractor.updateUserSetting(
                CHART_DRAW_DOTS,
                preferencesRepository.graphDrawDots().toString()
            )
        }
    }

    fun updateChartViewPeriod() {
        if (networkInteractor.signedIn) {
            networkInteractor.updateUserSetting(
                CHART_VIEW_PERIOD,
                preferencesRepository.getGraphViewPeriodDays().toString()
            )
        }
    }

    fun updatePressureUnit() {
        if (networkInteractor.signedIn) {
            networkInteractor.updateUserSetting(
                UNIT_PRESSURE,
                unitsConverter.getPressureUnit().code.toString()
            )
        }
    }

    companion object {
        val BACKGROUND_SCAN_MODE = "BACKGROUND_SCAN_MODE"
        val BACKGROUND_SCAN_INTERVAL = "BACKGROUND_SCAN_INTERVAL"
        val UNIT_TEMPERATURE = "UNIT_TEMPERATURE"
        val UNIT_HUMIDITY = "UNIT_HUMIDITY"
        val UNIT_PRESSURE = "UNIT_PRESSURE"
        val DASHBOARD_ENABLED = "DASHBOARD_ENABLED"
        val CHART_SHOW_ALL_POINTS = "CHART_SHOW_ALL_POINTS"
        val CHART_DRAW_DOTS = "CHART_DRAW_DOTS"
        val CHART_VIEW_PERIOD = "CHART_VIEW_PERIOD"
    }
}