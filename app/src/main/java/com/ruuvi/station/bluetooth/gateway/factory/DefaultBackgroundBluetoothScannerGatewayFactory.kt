package com.ruuvi.station.bluetooth.gateway.factory

import android.app.Application
import com.ruuvi.station.bluetooth.gateway.impl.AndroidBackgroundBluetoothScannerGateway

class DefaultBackgroundBluetoothScannerGatewayFactory(private val application: Application) : BackgroundBluetoothScannerGatewayFactory {

    override fun create(): AndroidBackgroundBluetoothScannerGateway = AndroidBackgroundBluetoothScannerGateway(application)
}