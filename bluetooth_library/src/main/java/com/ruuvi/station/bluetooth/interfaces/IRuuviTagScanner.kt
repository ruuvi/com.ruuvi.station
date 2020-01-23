package com.ruuvi.station.bluetooth.interfaces

import com.ruuvi.station.bluetooth.FoundRuuviTag

interface IRuuviTagScanner {

    fun startScanning(foundListener: OnTagFoundListener)

    fun stopScanning()

    fun canScan(): Boolean

    interface OnTagFoundListener {

        fun onTagFound(tag: FoundRuuviTag)
    }
}