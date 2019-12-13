package com.ruuvi.station.bluetooth.gateway

import com.ruuvi.station.bluetooth.model.LeScanResult
import com.ruuvi.station.scanning.RuuviTagListener

interface BluetoothScanningGateway {

    /**
     * Starts a low-energy scan which produces RuuviTag instances
     * (initial scan produces LeScanResult instances, which can be
     * parsed into ruuviTags : ruuviTag = leScanResult.parse(context)
     */
    fun startScan(listener: RuuviTagListener)

    /**
     * Starts a low-energy scan which produces LeScanResult instances
     */
    fun startScan(listener: LeScanResultListener)

    /**
     * Stops the currently started scan
     */
    fun stopScan()

    /**
     * Check if we can scan (devices are enabled, permissions granted etc.)
     */
    fun canScan(): Boolean

    /**
     * Start listening for range changes
     */
    fun listenForRangeChanges(rangeListener: RangeListener)

    interface RangeListener {

        fun onRangeChanged()
    }
}

interface LeScanResultListener {

    fun onLeScanResult(result: LeScanResult)
}