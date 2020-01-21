package com.ruuvi.station.bluetooth.interfaces

import com.ruuvi.station.bluetooth.interfaces.IRuuviTag

interface RuuviTagFactory {

    fun createTag(): IRuuviTag

}