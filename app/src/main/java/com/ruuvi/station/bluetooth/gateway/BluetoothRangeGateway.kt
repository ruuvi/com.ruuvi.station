package com.ruuvi.station.bluetooth.gateway

import com.ruuvi.station.model.RuuviTag

interface BluetoothRangeGateway {

    fun listenForRangeChanges(rangeListener: RangeListener)

    fun setBackgroundMode(isBackgroundModeEnabled: Boolean)

    fun stopScanning()

    fun reset()

    fun getBackgroundBetweenScanPeriod(): Long?

    fun startBackgroundScanning(): Boolean

    fun isForegroundScanningActive(): Boolean

    fun setEnableScheduledScanJobs(areScheduledScanJobsEnabled: Boolean)

    interface RangeListener {

        fun onFoundTags(allTags: List<RuuviTag>)
    }
}