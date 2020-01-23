package com.ruuvi.station

import android.app.Application
import com.raizlabs.android.dbflow.config.FlowManager
import com.ruuvi.station.bluetooth.domain.BluetoothInteractor
import com.ruuvi.station.bluetooth.domain.BluetoothScannerInteractor
import com.ruuvi.station.bluetooth.interfaces.IRuuviTagFactory
import com.ruuvi.station.bluetooth.domain.BluetoothForegroundScanningInteractor
import com.ruuvi.station.bluetooth.interfaces.IRuuviTag
import com.ruuvi.station.model.RuuviTag

class RuuviScannerApplication : Application() {

    val ruuviTagFactory = object : IRuuviTagFactory {
        override fun createTag(): IRuuviTag = RuuviTag()
    }
    val bluetoothScannerInteractor = BluetoothScannerInteractor(this, ruuviTagFactory)
    val bluetoothInteractor = BluetoothInteractor(this, ruuviTagFactory)

    val bluetoothForegroundServiceGateway = BluetoothForegroundScanningInteractor(this, ruuviTagFactory)

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