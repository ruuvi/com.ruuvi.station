package com.ruuvi.station.bluetooth

import android.app.Application
import com.ruuvi.station.util.Preferences

class BluetoothForegroundServiceGateway(private val application: Application) {

    private val ruuviRangeNotifier by lazy { RuuviRangeNotifier(application, "AltBeaconFGScannerService") }

    fun startScanning() {
        stopScanning()

        ruuviRangeNotifier
        ruuviRangeNotifier.startScan()
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