package com.ruuvi.station

import android.app.Application
import com.ruuvi.station.bluetooth.BluetoothForegroundScannerInteractor
import com.ruuvi.station.bluetooth.BluetoothForegroundServiceGateway
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.BluetoothScannerInteractor

class RuuviScannerApplication : Application() {

    val bluetoothForegroundScannerInteractor = BluetoothForegroundScannerInteractor(this)
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