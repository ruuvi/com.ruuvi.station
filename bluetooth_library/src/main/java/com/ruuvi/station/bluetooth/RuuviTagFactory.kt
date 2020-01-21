package com.ruuvi.station.bluetooth

import com.ruuvi.station.bluetooth.domain.IRuuviTag

interface RuuviTagFactory {

    fun createTag(): IRuuviTag

}