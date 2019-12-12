package com.ruuvi.station.bluetooth.gateway.factory

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context

class DefaultBluetoothScanningGatewayFactory(
    private val application: Application
) : BluetoothScanningGatewayFactory {

    override fun create(): BluetoothScanningGateway {

        val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        return NeovisionariesBluetoothScanningGateway(application, bluetoothManager)
    }
}