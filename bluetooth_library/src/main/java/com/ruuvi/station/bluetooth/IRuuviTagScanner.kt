package com.ruuvi.station.bluetooth

interface IRuuviTagScanner {

    fun startScanning(foundListener: OnTagFoundListener)

    fun stopScanning()

    fun canScan(): Boolean

    interface OnTagFoundListener {

        fun onTagFound(tag: FoundRuuviTag)
    }
}