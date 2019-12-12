package com.ruuvi.station.bluetooth.gateway.factory

import android.app.Application
import com.ruuvi.station.bluetooth.gateway.impl.ScannerServiceAndroidBluetoothGateway

class DefaultScannerServiceBluetoothGatewayFactory(
    private val application: Application
) : ScannerServiceBluetoothGatewayFactory {

    override fun create(): ScannerServiceAndroidBluetoothGateway = ScannerServiceAndroidBluetoothGateway(application)
}