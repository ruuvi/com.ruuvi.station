package com.ruuvi.station.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ruuvi.station.bluetooth.BluetoothForegroundService
import com.ruuvi.station.bluetooth.ScanningPeriodicReceiver
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.app.preferences.Preferences
import timber.log.Timber

class RebootSchedulerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("ruuvi.BOOT_COMPLETED.onReceive")
        context?.let {
            val preferences = Preferences(it)
            Timber.d("Start from reboot")
            if (preferences.backgroundScanMode == BackgroundScanModes.BACKGROUND) {
                ScanningPeriodicReceiver.start(it, preferences.backgroundScanInterval * 1000L)
                startForegroundService(it)
            } else {
                Timber.d("Background scan disabled")
            }
        }
    }

    private fun startForegroundService(context: Context) {
        Timber.d("startForegroundService after reboot")
        BluetoothForegroundService.start(context)
    }
}