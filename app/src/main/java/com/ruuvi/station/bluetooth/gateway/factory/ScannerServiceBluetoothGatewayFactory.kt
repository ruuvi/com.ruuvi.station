package com.ruuvi.station.bluetooth.gateway.factory

import com.ruuvi.station.bluetooth.gateway.impl.ScannerServiceAndroidBluetoothGateway

interface ScannerServiceBluetoothGatewayFactory {

    fun create(): ScannerServiceAndroidBluetoothGateway
}
