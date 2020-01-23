package com.ruuvi.station.bluetooth.domain

import android.app.Application
import com.ruuvi.station.bluetooth.DefaultOnTagFoundListener
import com.ruuvi.station.bluetooth.RuuviRangeNotifier
import com.ruuvi.station.util.Preferences

class BluetoothForegroundScanningInteractor(
    private val application: Application
) {

    private val ruuviRangeNotifier by lazy {
        DefaultOnTagFoundListener.gatewayOn = true

        RuuviRangeNotifier(
            application,
            "AltBeaconFGScannerService"
        )
    }

    fun startScanning() {
        stopScanning()

        ruuviRangeNotifier.startScanning(DefaultOnTagFoundListener(application), false)
    }

    fun enableForegroundMode() {
        ruuviRangeNotifier.enableScheduledScans(false)
    }

    fun enableBackgroundMode() {
       if(shouldUpdateScanInterval()){
           val scanInterval = Preferences(application).backgroundScanInterval * 1000
           ruuviRangeNotifier.setBackgroundScheduledScanInterval(scanInterval.toLong())
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