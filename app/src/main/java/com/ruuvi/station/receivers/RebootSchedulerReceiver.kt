package com.ruuvi.station.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ruuvi.station.bluetooth.ScanningPeriodicReceiver
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Preferences
import timber.log.Timber

class RebootSchedulerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("ruuvi.BOOT_COMPLETED.onReceive")
        context?.let {
            var prefs = Preferences(it)
            Timber.tag(this.javaClass.simpleName).d("Start from reboot")
            if (prefs.backgroundScanMode != BackgroundScanModes.DISABLED) {
                ScanningPeriodicReceiver.start(it, prefs.backgroundScanInterval * 1000L)
            } else {
                Timber.d("Background scan disabled")
            }
        }
    }
}