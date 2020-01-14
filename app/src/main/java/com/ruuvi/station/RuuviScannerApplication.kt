package com.ruuvi.station

import android.app.Application
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.BluetoothScannerInteractor
import com.ruuvi.station.bluetooth.BluetoothForegroundServiceGateway

class RuuviScannerApplication : Application() {

    val bluetoothScannerInteractor = BluetoothScannerInteractor(this)
    val bluetoothInteractor = BluetoothInteractor(this)

    val bluetoothForegroundServiceGateway = BluetoothForegroundServiceGateway(this)

    override fun onCreate() {
        super.onCreate()
        bluetoothInteractor.onCreate()
    }

    fun startForegroundScanning() {
        bluetoothInteractor.startForegroundScanning()
    }

    fun startBackgroundScanning() {
        bluetoothInteractor.startBackgroundScanning()
    }
}