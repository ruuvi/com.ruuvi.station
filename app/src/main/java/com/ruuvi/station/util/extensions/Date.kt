package com.ruuvi.station.util.extensions

import java.util.*
import kotlin.math.abs

fun Date.getEpochSecond(): Long {
    return this.time / 1000L
}

fun Date.diffGreaterThan(diff: Long): Boolean {
    return abs(Date().time - this.time) > diff
}