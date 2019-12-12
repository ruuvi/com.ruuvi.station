package com.ruuvi.station.bluetooth.gateway

import com.ruuvi.station.model.RuuviTag

interface ScannerServiceBluetoothGateway {

    fun startScan(discoveredTagListener: DiscoveredRuuviTagListener)

    fun stopScan()

    fun canScan(): Boolean

    interface DiscoveredRuuviTagListener {

        fun tagFound(tag: RuuviTag, foreground: Boolean)
    }
}