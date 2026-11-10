package com.ruuvi.station.receivers

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
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
    private fun isInForeground(): Boolean {
        val appProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE)
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("onReceive $intent")

        // The receiver is exported so the system can deliver boot broadcasts,
        // which also lets other apps target it with explicit intents.
        if (!isExpectedAction(intent?.action)) {
            Timber.w("Ignoring unexpected action ${intent?.action}")
            return
        }

        context?.let {
            if (isInForeground()) {
                Timber.d("Ignoring since app is in foreground")
            } else {
                val preferences = Preferences(it)
                Timber.d("Start from reboot")
                if (preferences.backgroundScanMode == BackgroundScanModes.BACKGROUND) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        ScanningPeriodicReceiver.start(it, preferences.backgroundScanInterval * 1000L)
                    }
                    startForegroundService(it)
                } else {
                    Timber.d("Background scan disabled")
                }
            }
        }
    }

    private fun startForegroundService(context: Context) {
        Timber.d("startForegroundService after reboot")
        BluetoothForegroundService.start(context)
    }

    companion object {
        // Keep in sync with the receiver's intent-filter in AndroidManifest.xml.
        private val EXPECTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )

        internal fun isExpectedAction(action: String?): Boolean = action in EXPECTED_ACTIONS
    }
}