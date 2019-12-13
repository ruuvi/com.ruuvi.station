package com.ruuvi.station.bluetooth.gateway

import com.ruuvi.station.model.RuuviTag

interface BluetoothTagGateway {

    fun listenForTags(onTagsFoundListener: OnTagsFoundListener)

    fun startBackgroundScanning(): Boolean

    fun stopScanning()

    /**
     * Reset the bluetooth services
     */
    fun reset()

    /**
     * Enable background scanning each {@link #getBackgroundBetweenScanInterval} milliseconds
     */
    fun setBackgroundMode(isBackgroundModeEnabled: Boolean)

    /**
     * @return is currently scanning in foreground
     */
    fun isForegroundScanningActive(): Boolean

    /**
     * @return interval in milliseconds for the background scan
     */
    fun getBackgroundBetweenScanInterval(): Long?

    interface OnTagsFoundListener {

        fun onFoundTags(allTags: List<RuuviTag>)
    }
}