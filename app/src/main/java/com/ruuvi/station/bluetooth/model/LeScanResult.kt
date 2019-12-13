package com.ruuvi.station.bluetooth.model

import android.content.Context
import com.ruuvi.station.model.RuuviTag

interface LeScanResult {

    /**
     * Parse a {@link LeScanResult} to produce a {@link RuuviTag} if possible,
     * may return null if could not produce a {@link RuuviTag}
     *
     * @param context The calling context
     *
     * @return a {@link RuuviTag}
     */
    fun parse(context: Context?): RuuviTag?

    /**
     * Compare two {@link LeScanResult} to check if they came from the same dev ice
     *
     * @return a boolean indicating if the same device has produced this result
     */
    fun hasSameDevice(otherScanResult: LeScanResult): Boolean
}