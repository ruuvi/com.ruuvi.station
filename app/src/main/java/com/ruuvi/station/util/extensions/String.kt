package com.ruuvi.station.util.extensions

private val HEX_CHARS = "0123456789ABCDEF"

fun String.hexStringToByteArray() : ByteArray {
    val input = this.toUpperCase()

    val result = ByteArray(length / 2)

    for (i in 0 until length step 2) {
        val firstIndex = HEX_CHARS.indexOf(input[i]);
        val secondIndex = HEX_CHARS.indexOf(input[i + 1]);

        val octet = firstIndex.shl(4).or(secondIndex)
        result.set(i.shr(1), octet.toByte())
    }

    return result
}