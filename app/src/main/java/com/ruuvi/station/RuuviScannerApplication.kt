package com.ruuvi.station

import android.app.Application
import com.raizlabs.android.dbflow.config.FlowManager
import com.ruuvi.station.bluetooth.domain.BluetoothForegroundScanningInteractor
import com.ruuvi.station.bluetooth.domain.BluetoothInteractor
import com.ruuvi.station.bluetooth.domain.BluetoothScannerInteractor

class RuuviScannerApplication : Application() {

    val bluetoothScannerInteractor = BluetoothScannerInteractor(this)
    val bluetoothInteractor = BluetoothInteractor(this)

    val bluetoothForegroundServiceGateway = BluetoothForegroundScanningInteractor(this)

    override fun onCreate() {
        super.onCreate()

        FlowManager.init(this)

        bluetoothInteractor.onAppCreated()
    }

    fun startForegroundScanning() {
        bluetoothInteractor.startForegroundScanning()
    }

    fun startBackgroundScanning() {
        bluetoothInteractor.startBackgroundScanning()
    }
}