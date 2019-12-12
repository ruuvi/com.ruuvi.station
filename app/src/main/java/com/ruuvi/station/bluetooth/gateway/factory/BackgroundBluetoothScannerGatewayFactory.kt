package com.ruuvi.station.bluetooth.gateway.factory

import com.ruuvi.station.bluetooth.gateway.BackgroundBluetoothScannerGateway

interface BackgroundBluetoothScannerGatewayFactory {

    fun create(): BackgroundBluetoothScannerGateway
}