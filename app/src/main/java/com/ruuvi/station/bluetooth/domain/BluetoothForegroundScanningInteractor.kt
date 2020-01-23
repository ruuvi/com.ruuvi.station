package com.ruuvi.station.bluetooth.domain

import android.app.Application
import com.ruuvi.station.bluetooth.DefaultOnTagFoundListener
import com.ruuvi.station.bluetooth.RuuviRangeNotifier
import com.ruuvi.station.bluetooth.interfaces.IRuuviTagFactory
import com.ruuvi.station.util.Preferences

class BluetoothForegroundScanningInteractor(
    private val application: Application,
    private val ruuviTagFactory: IRuuviTagFactory
) {

    private val ruuviRangeNotifier by lazy {
        DefaultOnTagFoundListener.gatewayOn = true

        RuuviRangeNotifier(
            application,
            ruuviTagFactory,
            "AltBeaconFGScannerService"
        )
    }

    fun startScanning() {
        stopScanning()

        ruuviRangeNotifier.startScanning(DefaultOnTagFoundListener(application), false)
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