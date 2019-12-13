package com.ruuvi.station.bluetooth.gateway.factory

import android.app.Application
import com.ruuvi.station.bluetooth.gateway.BluetoothRangeGateway
import com.ruuvi.station.bluetooth.gateway.impl.AltBeaconBluetoothRangeGateway

class DefaultBluetoothRangeGatewayFactory(private val application: Application) : BluetoothRangeGatewayFactory {

    override fun create(): BluetoothRangeGateway =
        AltBeaconBluetoothRangeGateway(application)
}