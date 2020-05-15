package com.ruuvi.station.settings.ui

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Build
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.util.BackgroundScanModes

class AppSettingsBackgroundScanViewModel (private val preferences: Preferences): ViewModel() {
    private val scanMode = MutableLiveData<BackgroundScanModes>()
    private val interval = MutableLiveData<Int>()

    init {
        scanMode.value = preferences.backgroundScanMode
        interval.value = preferences.backgroundScanInterval
    }

    fun observeScanMode(): LiveData<BackgroundScanModes> = scanMode

    fun observeInterval(): LiveData<Int> = interval

    fun setBackgroundMode(mode: BackgroundScanModes){
        preferences.backgroundScanMode = mode
    }

    fun setBackgroundScanInterval(newInterval: Int) {
        preferences.backgroundScanInterval = newInterval
    }

    fun getPossibleScanModes() = listOf(
            BackgroundScanModes.DISABLED,
            BackgroundScanModes.BACKGROUND
    )

    fun getBatteryOptimizationMessageId(): Int {
        val deviceManufacturer = Build.MANUFACTURER.toUpperCase()
        val deviceApi = Build.VERSION.SDK_INT

        return when (deviceManufacturer) {
            SAMSUNG_MANUFACTURER ->
                if (deviceApi <= Build.VERSION_CODES.M) {
                    R.string.background_scan_samsung23_instructions
                } else {
                    R.string.background_scan_samsung_instructions
                }
            XIAOMI_MANUFACTURER -> R.string.background_scan_xiaomi_instructions
            HUAWEI_MANUFACTURER -> R.string.background_scan_huawei_instructions
            else -> R.string.background_scan_common_instructions
        }
    }

    companion object {
        const val SAMSUNG_MANUFACTURER = "SAMSUNG"
        const val XIAOMI_MANUFACTURER = "XIAOMI"
        const val HUAWEI_MANUFACTURER = "HUAWEI"
    }
}