package com.ruuvi.station.bluetooth.gateway.factory

import android.app.Application
import com.ruuvi.station.bluetooth.gateway.impl.BackgroundAndroidBluetoothScannerGateway

class DefaultBackgroundBluetoothScannerGatewayFactory(private val application: Application) : BackgroundBluetoothScannerGatewayFactory {

    override fun create(): BackgroundAndroidBluetoothScannerGateway = BackgroundAndroidBluetoothScannerGateway(application)
}