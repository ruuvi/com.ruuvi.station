package com.ruuvi.station.bluetooth.gateway.factory

import com.ruuvi.station.bluetooth.gateway.BluetoothRangeGateway

interface BluetoothRangeGatewayFactory {

    fun create(): BluetoothRangeGateway
}