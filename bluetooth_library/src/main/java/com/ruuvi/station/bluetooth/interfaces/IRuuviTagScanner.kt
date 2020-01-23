package com.ruuvi.station.bluetooth.interfaces

interface IRuuviTagScanner {

    fun start()

    fun stop()

    fun canScan(): Boolean
}