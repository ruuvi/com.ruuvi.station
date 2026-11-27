package com.ruuvi.station.history

/** Unknown cloud allowances never authorize deletion. Free accounts keep their local BLE history. */
object HistoryRetention {
    const val LOCAL_DAYS = 10
    fun days(cloud: Boolean, allowance: Int?): Int? = when {
        !cloud -> LOCAL_DAYS
        allowance == null || allowance < 0 -> null
        allowance == 0 -> LOCAL_DAYS
        else -> allowance
    }
    fun cutoffMillis(now: Long, cloud: Boolean, allowance: Int?): Long? =
        days(cloud, allowance)?.let { now - it.toLong() * 86_400_000L }
}
