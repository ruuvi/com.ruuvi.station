package com.ruuvi.station.bluetooth.gateway.factory

import com.ruuvi.station.bluetooth.gateway.BluetoothScanningGateway

interface BluetoothScanningGatewayFactory {

    fun create(): BluetoothScanningGateway
}
