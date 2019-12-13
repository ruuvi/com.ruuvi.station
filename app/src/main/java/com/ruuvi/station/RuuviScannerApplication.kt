package com.ruuvi.station

import android.app.Application
import android.util.Log
import com.raizlabs.android.dbflow.config.FlowManager
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.gateway.factory.BackgroundBluetoothScannerGatewayFactory
import com.ruuvi.station.bluetooth.gateway.factory.BluetoothTagGatewayFactory
import com.ruuvi.station.bluetooth.gateway.factory.BluetoothScanningGatewayFactory
import com.ruuvi.station.bluetooth.gateway.factory.DefaultBackgroundBluetoothScannerGatewayFactory
import com.ruuvi.station.bluetooth.gateway.factory.DefaultBluetoothTagGatewayFactory
import com.ruuvi.station.bluetooth.gateway.factory.DefaultBluetoothScanningGatewayFactory
import com.ruuvi.station.bluetooth.model.factory.DefaultLeScanResultFactory
import com.ruuvi.station.bluetooth.model.factory.LeScanResultFactory

class RuuviScannerApplication : Application() {

    val leScanResultFactory: LeScanResultFactory by lazy { DefaultLeScanResultFactory(this) }

    val scannerGatewayFactory: BackgroundBluetoothScannerGatewayFactory by lazy { DefaultBackgroundBluetoothScannerGatewayFactory(this) }

    val bluetoothScanningGatewayFactory: BluetoothScanningGatewayFactory by lazy { DefaultBluetoothScanningGatewayFactory(this) }

    val bluetoothRangeGatewayFactory: BluetoothTagGatewayFactory by lazy { DefaultBluetoothTagGatewayFactory(this) }

    val bluetoothInteractor by lazy { BluetoothInteractor(this) }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "App class onCreate")
        bluetoothInteractor.onAppCreated()
        FlowManager.init(applicationContext)
    }

    companion object {
        private const val TAG = "RuuviScannerApplication"
    }
}