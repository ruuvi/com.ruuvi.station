package com.ruuvi.station.app.domain

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.PowerManager
import android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS

class PowerManagerInterator(
    val context: Context,
    val powerManager: PowerManager
) {
    fun isIgnoringBatteryOptimizations() = powerManager.isIgnoringBatteryOptimizations(context.packageName)

    fun openOptimizationSettings() {
        val intent = Intent().also {
            it.action = ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            it.flags = FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}