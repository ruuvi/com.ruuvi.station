package com.ruuvi.station.bluetooth.gateway.factory

import com.ruuvi.station.bluetooth.gateway.BluetoothTagGateway

interface BluetoothTagGatewayFactory {

    fun create(): BluetoothTagGateway
}