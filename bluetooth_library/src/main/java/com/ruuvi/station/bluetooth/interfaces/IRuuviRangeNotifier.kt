package com.ruuvi.station.bluetooth.interfaces

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

        fun onFoundTags(allTags: List<IRuuviTag>)
    }
}