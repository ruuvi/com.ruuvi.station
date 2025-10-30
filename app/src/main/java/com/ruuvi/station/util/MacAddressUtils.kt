package com.ruuvi.station.util

import java.util.*

class MacAddressUtils {
    companion object {
        fun incrementMacAddress(mac: String): String {
            if (!macIsValid(mac)) throw IllegalArgumentException("Invalid MAC-address")

            val inputHex = mac.replace(":","")
            val incrementedValue = inputHex.toLong(16) + 1
            val list = Regex(MAC_VALUE_GROUPS_PATTERN).findAll(incrementedValue.toString(16)).map { it.value }
            return list.joinToString(separator = ":", transform = { it.uppercase(Locale.ROOT) })
        }

        fun macIsValid(mac: String) = Regex(MAC_PATTERN).matches(mac)

        fun getDefaultName(id: String, isAir: Boolean?): String {
            return when (isAir) {
                true -> "Ruuvi Air ${id.takeLast(5).removeRange(2,3)}"
                false -> "RuuviTag ${id.takeLast(5).removeRange(2,3)}"
                null -> "Ruuvi ${id.takeLast(5).removeRange(2,3)}"
            }
        }

        const val MAC_PATTERN = "^(([\\d,A-F,a-f]){2}:){5}([\\d,A-F,a-f]){2}\$"
        const val MAC_VALUE_GROUPS_PATTERN = "[a-fA-F\\d][a-fA-F\\d]"
    }
}