package com.ruuvi.station

import android.app.Application
import com.ruuvi.station.bluetooth.domain.BluetoothForegroundScanningInteractor
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.BluetoothScannerInteractor

class RuuviScannerApplication : Application() {

    val bluetoothScannerInteractor = BluetoothScannerInteractor(this)
    val bluetoothInteractor = BluetoothInteractor(this)

    val bluetoothForegroundServiceGateway = BluetoothForegroundScanningInteractor(this)

    override fun onCreate() {
        super.onCreate()
        bluetoothInteractor.onAppCreated()
    }

    fun startForegroundScanning() {
        bluetoothInteractor.startForegroundScanning()
    }

    fun startBackgroundScanning() {
        bluetoothInteractor.startBackgroundScanning()
    }
}