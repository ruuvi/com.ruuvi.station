package com.ruuvi.station.util

import java.util.UUID

object DeviceIdGenerator {

    fun generateId(): String =
        (UUID.randomUUID().mostSignificantBits.toString(36) +
            UUID.randomUUID().leastSignificantBits.toString(36))
            .substring(1)
}