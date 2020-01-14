package com.ruuvi.station.bluetooth

import android.app.Application

class BluetoothForegroundScannerInteractor(private val application: Application) {

    private lateinit var ruuviTagScanner: RuuviTagScanner

    fun startScan(ruuviTagListener: RuuviTagListener) {

        stopScan()

        ruuviTagScanner = RuuviTagScanner(ruuviTagListener, application)
        ruuviTagScanner.start()
    }

    fun stopScan() {
        if (::ruuviTagScanner.isInitialized) {
            ruuviTagScanner.stop()
        }
    }
}