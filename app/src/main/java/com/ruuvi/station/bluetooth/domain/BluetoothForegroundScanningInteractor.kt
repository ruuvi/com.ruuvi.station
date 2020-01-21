package com.ruuvi.station.bluetooth.domain

import android.app.Application
import com.ruuvi.station.bluetooth.DefaultOnTagFoundListener
import com.ruuvi.station.bluetooth.RuuviRangeNotifier
import com.ruuvi.station.bluetooth.RuuviTagFactory
import com.ruuvi.station.util.Preferences

class BluetoothForegroundScanningInteractor(
    private val application: Application,
    private val ruuviTagFactory: RuuviTagFactory
) {

    private val ruuviRangeNotifier by lazy {
        RuuviRangeNotifier(
            application,
            ruuviTagFactory,
            "AltBeaconFGScannerService"
        )
    }

    fun startScanning() {
        stopScanning()

        ruuviRangeNotifier.startScan(DefaultOnTagFoundListener(application))
    }

    fun enableForegroundMode() {
        ruuviRangeNotifier.setEnableScheduledScanJobs(false)
    }

    fun enableBackgroundMode() {
       if(shouldUpdateScanInterval()){
           val scanInterval = Preferences(application).backgroundScanInterval * 1000
           ruuviRangeNotifier.setBackgroundScanInterval(scanInterval.toLong())
        }
        ruuviRangeNotifier.enableBackgroundMode(true)
    }

    fun stopScanning() {
        ruuviRangeNotifier.stopScanning()
    }

    fun onBecameForeground() {
       ruuviRangeNotifier.enableBackgroundMode(false)
    }

    fun shouldUpdateScanInterval(): Boolean {
        val scanInterval = Preferences(application).backgroundScanInterval * 1000
        return scanInterval.toLong() != ruuviRangeNotifier.getBackgroundScanInterval()
    }
}