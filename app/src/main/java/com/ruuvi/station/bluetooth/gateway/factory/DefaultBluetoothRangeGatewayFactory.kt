package com.ruuvi.station.bluetooth.gateway.factory

import android.app.Application
import com.ruuvi.station.bluetooth.gateway.BluetoothTagGateway
import com.ruuvi.station.bluetooth.gateway.impl.AltBeaconBluetoothTagGateway

class DefaultBluetoothRangeGatewayFactory(private val application: Application) : BluetoothRangeGatewayFactory {

    override fun create(): BluetoothTagGateway =
        AltBeaconBluetoothTagGateway(application)
}