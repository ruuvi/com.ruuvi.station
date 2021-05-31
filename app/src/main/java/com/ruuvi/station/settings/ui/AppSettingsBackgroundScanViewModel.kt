package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import android.os.Build
import com.ruuvi.station.R
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.util.BackgroundScanModes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class AppSettingsBackgroundScanViewModel(
    private val interactor: AppSettingsInteractor
) : ViewModel() {

    private val scanMode = MutableStateFlow<BackgroundScanModes?>(null)
    val scanModeFlow: StateFlow<BackgroundScanModes?> = scanMode

    private val interval = MutableStateFlow(0)
    val intervalFlow: StateFlow<Int?> = interval

    init {
        scanMode.value = interactor.getBackgroundScanMode()
        interval.value = interactor.getBackgroundScanInterval()
    }

    fun setBackgroundMode(mode: BackgroundScanModes) =
        interactor.setBackgroundScanMode(mode)

    fun setBackgroundScanInterval(newInterval: Int) =
        interactor.setBackgroundScanInterval(newInterval)

    fun getPossibleScanModes() = listOf(
        BackgroundScanModes.DISABLED,
        BackgroundScanModes.BACKGROUND
    )

    fun getBatteryOptimizationMessageId(): Int {
        val deviceManufacturer = Build.MANUFACTURER.toUpperCase(Locale.getDefault())
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

    companion object {
        const val SAMSUNG_MANUFACTURER = "SAMSUNG"
        const val XIAOMI_MANUFACTURER = "XIAOMI"
        const val HUAWEI_MANUFACTURER = "HUAWEI"
    }
}