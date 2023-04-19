package com.ruuvi.station.network.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.network.data.response.NetworkUserSettings
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.units.model.TemperatureUnit
import com.ruuvi.station.util.BackgroundScanModes
import timber.log.Timber

class NetworkApplicationSettings (
    private val tokenRepository: NetworkTokenRepository,
    private val networkRepository: RuuviNetworkRepository,
    private val networkInteractor: RuuviNetworkInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val unitsConverter: UnitsConverter
    ) {

    private fun getToken() = tokenRepository.getTokenInfo()

    suspend fun updateSettingsFromNetwork() {

        getToken()?.token?.let { token ->
            val response = networkRepository.getUserSettings(token)
            if (response?.data != null && response.isSuccess()) {
                if (initializeSettings(response.data.settings)) {
                    applyBackgroundScanMode(response.data.settings)
                    applyBackgroundScanInterval(response.data.settings)
                    applyTemperatureUnit(response.data.settings)
                    applyHumidityUnit(response.data.settings)
                    applyPressureUnit(response.data.settings)
                    applyDashboardType(response.data.settings)
                    applyDashboardTapAction(response.data.settings)
                    applyCloudModeEnabled(response.data.settings)
                    applyChartShowAllPoints(response.data.settings)
                    applyChartDrawDots(response.data.settings)
                    applyChartViewPeriod(response.data.settings)
                    applyTemperatureAccuracy(response.data.settings)
                    applyHumidityAccuracy(response.data.settings)
                    applyPressureAccuracy(response.data.settings)
                }
            }
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
            updateDashboardType()
            updateDashboardTapAction()
            updateChartShowAllPoints()
            updateCloudModeEnabled()
            updateChartDrawDots()
            updateChartViewPeriod()
            updateTemperatureAccuracy()
            updateHumidityAccuracy()
            updatePressureAccuracy()
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

    private fun applyDashboardType(settings: NetworkUserSettings) {
        if (settings.DASHBOARD_TYPE != null && DashboardType.isValidCode(settings.DASHBOARD_TYPE)) {
            Timber.d("NetworkApplicationSettings-applyDashboardType: ${settings.DASHBOARD_TYPE}")
            preferencesRepository.updateDashboardType(DashboardType.getByCode(settings.DASHBOARD_TYPE))
        }
    }

    private fun applyDashboardTapAction(settings: NetworkUserSettings) {
        if (settings.DASHBOARD_TAP_ACTION != null && DashboardTapAction.isValidCode(settings.DASHBOARD_TAP_ACTION)) {
            Timber.d("NetworkApplicationSettings-applyDashboardTapAction: ${settings.DASHBOARD_TAP_ACTION}")
            preferencesRepository.updateDashboardTapAction(DashboardTapAction.getByCode(settings.DASHBOARD_TAP_ACTION))
        }
    }

    private fun applyCloudModeEnabled(settings: NetworkUserSettings) {
        if (settings.CLOUD_MODE_ENABLED != null) {
            settings.CLOUD_MODE_ENABLED.toBoolean().let {
                Timber.d("NetworkApplicationSettings-applyCloudModeEnabled: $it")
                preferencesRepository.setIsCloudModeEnabled(it)
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

    private fun applyTemperatureAccuracy(settings: NetworkUserSettings) {
        settings.ACCURACY_TEMPERATURE?.toIntOrNull()?.let {
            val accuracy = Accuracy.getByCode(it)
            if (accuracy != null) {
                Timber.d("NetworkApplicationSettings-applyTemperatureAccuracy: $accuracy")
                preferencesRepository.setTemperatureAccuracy(accuracy)
            }
        }
    }

    private fun applyHumidityAccuracy(settings: NetworkUserSettings) {
        settings.ACCURACY_HUMIDITY?.toIntOrNull()?.let {
            val accuracy = Accuracy.getByCode(it)
            if (accuracy != null) {
                Timber.d("NetworkApplicationSettings-applyHumidityAccuracy: $accuracy")
                preferencesRepository.setHumidityAccuracy(accuracy)
            }
        }
    }

    private fun applyPressureAccuracy(settings: NetworkUserSettings) {
        settings.ACCURACY_PRESSURE?.toIntOrNull()?.let {
            val accuracy = Accuracy.getByCode(it)
            if (accuracy != null) {
                Timber.d("NetworkApplicationSettings-applyPressureAccuracy: $accuracy")
                preferencesRepository.setPressureAccuracy(accuracy)
            }
        }
    }

    fun updateBackgroundScanMode() {
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateBackgroundScanMode: ${preferencesRepository.getBackgroundScanMode().value}")
            networkInteractor.updateUserSetting(
                BACKGROUND_SCAN_MODE,
                preferencesRepository.getBackgroundScanMode().value.toString()
            )
        }
    }

    fun updateTemperatureUnit() {
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateTemperatureUnit: ${unitsConverter.getTemperatureUnit().code}")
            networkInteractor.updateUserSetting(
                UNIT_TEMPERATURE,
                unitsConverter.getTemperatureUnit().code
            )
        }
    }

    fun updateTemperatureAccuracy() {
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateTemperatureAccuracy: ${preferencesRepository.getTemperatureAccuracy().code}")
            networkInteractor.updateUserSetting(
                ACCURACY_TEMPERATURE,
                preferencesRepository.getTemperatureAccuracy().code.toString()
            )
        }
    }

    fun updateHumidityUnit() {
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateHumidityUnit: ${unitsConverter.getHumidityUnit().code}")
            networkInteractor.updateUserSetting(
                UNIT_HUMIDITY,
                unitsConverter.getHumidityUnit().code.toString()
            )
        }
    }

    fun updateHumidityAccuracy() {
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateHumidityAccuracy: ${preferencesRepository.getHumidityAccuracy().code}")
            networkInteractor.updateUserSetting(
                ACCURACY_HUMIDITY,
                preferencesRepository.getHumidityAccuracy().code.toString()
            )
        }
    }

    fun updateDashboardType() {
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateDashboardType: ${preferencesRepository.getDashboardType().code}")
            networkInteractor.updateUserSetting(
                DASHBOARD_TYPE,
                preferencesRepository.getDashboardType().code
            )
        }
    }

    fun updateCloudModeEnabled() {
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateCloudModeEnabled: ${preferencesRepository.isCloudModeEnabled()}")
            networkInteractor.updateUserSetting(
                CLOUD_MODE_ENABLED,
                preferencesRepository.isCloudModeEnabled().toString()
            )
        }
    }

    fun updateBackgroundScanInterval() {
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateBackgroundScanInterval: ${preferencesRepository.getBackgroundScanInterval()}")
            networkInteractor.updateUserSetting(
                BACKGROUND_SCAN_INTERVAL,
                preferencesRepository.getBackgroundScanInterval().toString()
            )
        }
    }

    fun updateChartShowAllPoints() {
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateChartShowAllPoints: ${preferencesRepository.isShowAllGraphPoint()}")
            networkInteractor.updateUserSetting(
                CHART_SHOW_ALL_POINTS,
                preferencesRepository.isShowAllGraphPoint().toString()
            )
        }
    }

    fun updateChartDrawDots() {
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateChartDrawDots: ${preferencesRepository.graphDrawDots()}")
            networkInteractor.updateUserSetting(
                CHART_DRAW_DOTS,
                preferencesRepository.graphDrawDots().toString()
            )
        }
    }

    fun updateChartViewPeriod() {
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateChartViewPeriod: ${preferencesRepository.getGraphViewPeriodDays()}")
            networkInteractor.updateUserSetting(
                CHART_VIEW_PERIOD,
                preferencesRepository.getGraphViewPeriodDays().toString()
            )
        }
    }

    fun updatePressureUnit() {
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updatePressureUnit: ${unitsConverter.getPressureUnit().code}")
            networkInteractor.updateUserSetting(
                UNIT_PRESSURE,
                unitsConverter.getPressureUnit().code.toString()
            )
        }
    }

    fun updatePressureAccuracy() {
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updatePressureAccuracy: ${preferencesRepository.getPressureAccuracy().code}")
            networkInteractor.updateUserSetting(
                ACCURACY_PRESSURE,
                preferencesRepository.getPressureAccuracy().code.toString()
            )
        }
    }

    fun updateDashboardTapAction() {
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateDashboardTapAction: ${preferencesRepository.getDashboardTapAction().code}")
            networkInteractor.updateUserSetting(
                DASHBOARD_TAP_ACTION,
                preferencesRepository.getDashboardTapAction().code
            )
        }
    }

    companion object {
        val BACKGROUND_SCAN_MODE = "BACKGROUND_SCAN_MODE"
        val BACKGROUND_SCAN_INTERVAL = "BACKGROUND_SCAN_INTERVAL"
        val UNIT_TEMPERATURE = "UNIT_TEMPERATURE"
        val UNIT_HUMIDITY = "UNIT_HUMIDITY"
        val UNIT_PRESSURE = "UNIT_PRESSURE"
        val ACCURACY_TEMPERATURE = "ACCURACY_TEMPERATURE"
        val ACCURACY_HUMIDITY = "ACCURACY_HUMIDITY"
        val ACCURACY_PRESSURE = "ACCURACY_PRESSURE"
        val DASHBOARD_TYPE = "DASHBOARD_TYPE"
        val DASHBOARD_TAP_ACTION = "DASHBOARD_TAP_ACTION"
        val CLOUD_MODE_ENABLED = "CLOUD_MODE_ENABLED"
        val CHART_SHOW_ALL_POINTS = "CHART_SHOW_ALL_POINTS"
        val CHART_DRAW_DOTS = "CHART_DRAW_DOTS"
        val CHART_VIEW_PERIOD = "CHART_VIEW_PERIOD"
    }
}