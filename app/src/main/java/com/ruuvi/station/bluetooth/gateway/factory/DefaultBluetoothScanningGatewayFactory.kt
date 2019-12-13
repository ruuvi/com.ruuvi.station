package com.ruuvi.station.bluetooth.gateway.factory

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import com.ruuvi.station.bluetooth.gateway.BluetoothScanningGateway
import com.ruuvi.station.bluetooth.gateway.impl.NeovisionariesBluetoothScanningGateway

class DefaultBluetoothScanningGatewayFactory(
    private val application: Application
) : BluetoothScanningGatewayFactory {

    override fun create(): BluetoothScanningGateway {

        val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        return NeovisionariesBluetoothScanningGateway(application, bluetoothManager)
    }
}