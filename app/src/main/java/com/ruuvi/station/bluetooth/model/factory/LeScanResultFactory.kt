package com.ruuvi.station.bluetooth.model.factory

import com.ruuvi.station.bluetooth.model.LeScanResult

interface LeScanResultFactory {

    /**
     * Returns a scan result produced from bluetooth scan data
     *
     * @return Bluetooth low-energy scan result
     */
    fun create(deviceAddress: String, rssi: Int, data: ByteArray): LeScanResult
}