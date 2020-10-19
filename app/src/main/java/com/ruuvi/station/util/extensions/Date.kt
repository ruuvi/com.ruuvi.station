package com.ruuvi.station.util.extensions

import java.util.*

fun Date.getEpochSecond(): Long {
    return this.time / 1000L
}