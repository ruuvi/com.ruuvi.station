package com.ruuvi.station.bluetooth.model

import android.content.Context
import com.ruuvi.station.model.RuuviTag

interface LeScanResult {

    fun parse(context: Context?): RuuviTag?

    fun hasSameDevice(otherScanResult: LeScanResult): Boolean
}