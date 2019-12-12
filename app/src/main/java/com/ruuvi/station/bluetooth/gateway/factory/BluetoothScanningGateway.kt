package com.ruuvi.station.bluetooth.gateway.factory

import com.ruuvi.station.bluetooth.model.LeScanResult
import com.ruuvi.station.scanning.RuuviTagListener

interface BluetoothScanningGateway {

    fun startScan(listener: RuuviTagListener)

    fun startScan(listener: LeScanResultListener)

    fun stopScan()

    fun canScan(): Boolean
}

interface LeScanResultListener {

    fun onLeScanResult(result: LeScanResult)
}