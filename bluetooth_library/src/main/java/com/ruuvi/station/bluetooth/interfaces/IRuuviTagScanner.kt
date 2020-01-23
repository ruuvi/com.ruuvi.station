package com.ruuvi.station.bluetooth.interfaces

import com.ruuvi.station.bluetooth.FoundRuuviTag

interface IRuuviTagScanner {

    fun startScanning(listener: RuuviTagListener)

    fun stopScanning()

    fun canScan(): Boolean

    interface RuuviTagListener {

        fun tagFound(tag: FoundRuuviTag)
    }
}