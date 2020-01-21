package com.ruuvi.station

import android.app.Application
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.BluetoothScannerInteractor
import com.ruuvi.station.bluetooth.RuuviTagFactory
import com.ruuvi.station.bluetooth.domain.BluetoothForegroundScanningInteractor
import com.ruuvi.station.bluetooth.domain.IRuuviTag
import com.ruuvi.station.model.RuuviTag

class RuuviScannerApplication : Application() {

    val ruuviTagFactory = object : RuuviTagFactory {
        override fun createTag(): IRuuviTag = RuuviTag()
    }
    val bluetoothScannerInteractor = BluetoothScannerInteractor(this, ruuviTagFactory)
    val bluetoothInteractor = BluetoothInteractor(this, ruuviTagFactory)

    val bluetoothForegroundServiceGateway = BluetoothForegroundScanningInteractor(this, ruuviTagFactory)

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