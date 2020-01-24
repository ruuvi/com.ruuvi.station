package com.ruuvi.station.bluetooth

interface IRuuviRangeNotifier {

    fun startScanning(
        tagsFoundListener: OnTagsFoundListener,
        shouldLaunchInBackground: Boolean
    )

    fun stopScanning()

    /*
     * Enable background scanning
     */
    fun enableBackgroundMode(isBackgroundModeEnabled: Boolean)

    /*
     * Get interval between scan jobs in milliseconds if it is already set
     */
    fun getBackgroundScanInterval(): Long?

    /*
     * Set interval between scan jobs in milliseconds
     */
    fun setBackgroundScheduledScanInterval(scanInterval: Long)

    /*
    * Enable scheduled scans every *setBackgroundScheduledScanInterval* milliseconds
    */
    fun enableScheduledScans(areScheduledScansEnabled: Boolean)

    interface OnTagsFoundListener {

        fun onTagsFound(allTags: List<FoundRuuviTag>)
    }
}