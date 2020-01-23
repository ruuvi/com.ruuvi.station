package com.ruuvi.station.bluetooth.interfaces

import com.ruuvi.station.bluetooth.FoundRuuviTag

interface IRuuviRangeNotifier {

    fun startScanning(
        tagsFoundListener: OnTagsFoundListener,
        shouldLaunchInBackground: Boolean
    )

    fun stopScanning()

    fun enableBackgroundMode(isBackgroundModeEnabled: Boolean)

    fun getBackgroundScanInterval(): Long?

    fun setBackgroundScheduledScanInterval(scanInterval: Long)

    fun enableScheduledScans(areScheduledScansEnabled: Boolean)

    interface OnTagsFoundListener {

        fun onTagsFound(allTags: List<FoundRuuviTag>)
    }
}