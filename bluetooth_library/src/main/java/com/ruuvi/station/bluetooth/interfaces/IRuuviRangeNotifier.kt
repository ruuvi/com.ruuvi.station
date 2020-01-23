package com.ruuvi.station.bluetooth.interfaces

import com.ruuvi.station.bluetooth.FoundRuuviTag

interface IRuuviRangeNotifier {

    fun startScanning(
        tagsFoundListener: OnTagsFoundListener,
        shouldLaunchInBackground: Boolean,
        backgroundScanIntervalMilliseconds: Long? = null
    )

    fun stopScanning()

    fun enableBackgroundMode(isBackgroundModeEnabled: Boolean)

    fun getBackgroundScanInterval(): Long?

    fun setEnableScheduledScanJobs(areScheduledScanJobsEnabled: Boolean)

    fun setBackgroundScanInterval(scanInterval: Long)

    interface OnTagsFoundListener {

        fun onFoundTags(allTags: List<FoundRuuviTag>)
    }
}