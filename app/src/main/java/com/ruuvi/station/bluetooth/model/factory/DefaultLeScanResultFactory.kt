package com.ruuvi.station.bluetooth.model.factory

import android.app.Application
import com.ruuvi.station.bluetooth.model.LeScanResult
import com.ruuvi.station.bluetooth.model.NeovisionariesLeScanResult

class DefaultLeScanResultFactory(private val application: Application) : LeScanResultFactory {

    override fun create(
        deviceAddress: String,
        rssi: Int,
        data: ByteArray
    ): LeScanResult {

        val leScanResult = NeovisionariesLeScanResult()
        leScanResult.deviceAddress = deviceAddress
        leScanResult.rssi = rssi
        leScanResult.scanData = data

        return leScanResult
    }
}