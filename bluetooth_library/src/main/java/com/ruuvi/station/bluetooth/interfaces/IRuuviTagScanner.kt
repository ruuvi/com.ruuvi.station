package com.ruuvi.station.bluetooth.interfaces

import com.ruuvi.station.bluetooth.FoundRuuviTag

interface IRuuviTagScanner {

    fun start(listener: RuuviTagListener)

    fun stop()

    fun canScan(): Boolean

    interface RuuviTagListener {

        fun tagFound(tag: FoundRuuviTag)
    }
}