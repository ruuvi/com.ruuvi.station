package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import android.os.Build
import com.ruuvi.station.R
import com.ruuvi.station.app.domain.PowerManagerInterator
import com.ruuvi.station.app.ui.components.SelectionElement
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class BackgroundScanSettingsViewModel(
    private val interactor: AppSettingsInteractor,
    private val powerManagerInterator: PowerManagerInterator
) : ViewModel() {

    private val scanMode = MutableStateFlow<BackgroundScanModes?>(null)
    val scanModeFlow: StateFlow<BackgroundScanModes?> = scanMode

    private val _backgroundScanEnabled = MutableStateFlow<Boolean> (
        interactor.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND
    )
    val backgroundScanEnabled: StateFlow<Boolean> = _backgroundScanEnabled

    private val interval = MutableStateFlow(interactor.getBackgroundScanInterval())
    val intervalFlow: StateFlow<Int?> = interval

    val showOptimizationTips: StateFlow<Boolean> =
        MutableStateFlow(!powerManagerInterator.isIgnoringBatteryOptimizations())

    init {
        scanMode.value = interactor.getBackgroundScanMode()
        interval.value = interactor.getBackgroundScanInterval()
    }

    fun getIntervalOptions(): List<SelectionElement> {
        val options: MutableList<SelectionElement> = mutableListOf()
        options.add(SelectionElement(10, 10, R.string.background_interval_10sec))
        for (i in 1..60) {
            options.add(SelectionElement(60 * i, i, R.string.background_interval))
        }
        return options
    }

    fun setBackgroundScanInterval(newInterval: Int) {
        interactor.setBackgroundScanInterval(newInterval)
        interval.value = interactor.getBackgroundScanInterval()
    }

    fun getBatteryOptimizationMessageId(): Int {
        val deviceManufacturer = Build.MANUFACTURER.uppercase(Locale.getDefault())
        val deviceApi = Build.VERSION.SDK_INT

        return when (deviceManufacturer) {
            SAMSUNG_MANUFACTURER ->
                if (deviceApi <= Build.VERSION_CODES.M) {
                    R.string.settings_background_battery_optimization_samsung23_instructions
                } else {
                    R.string.settings_background_battery_optimization_samsung_instructions
                }
            XIAOMI_MANUFACTURER -> R.string.settings_background_battery_optimization_xiaomi_instructions
            HUAWEI_MANUFACTURER -> R.string.settings_background_battery_optimization_huawei_instructions
            else -> R.string.settings_background_battery_optimization_common_instructions
        }
    }

    fun setBackgroundScanEnabled(enabled: Boolean) {
        interactor.setBackgroundScanMode(if (enabled) BackgroundScanModes.BACKGROUND else BackgroundScanModes.DISABLED)
        _backgroundScanEnabled.value = interactor.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND
    }

    fun openOptimizationSettings() {
        powerManagerInterator.openOptimizationSettings()
    }
}