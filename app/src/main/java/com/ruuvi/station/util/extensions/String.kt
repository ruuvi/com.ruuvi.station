package com.ruuvi.station.util.extensions

import java.util.Locale

private const val HEX_CHARS = "0123456789ABCDEF"
private val FORBIDDEN_CHARS = arrayOf('"', '*', '/', ':', '<', '>', '?', '\\', '|')

fun String.hexStringToByteArray(): ByteArray {
    val input = this.toUpperCase(Locale.getDefault())

    val result = ByteArray(length / 2)

    for (i in 0 until length step 2) {
        val firstIndex = HEX_CHARS.indexOf(input[i])
        val secondIndex = HEX_CHARS.indexOf(input[i + 1])

        val octet = firstIndex.shl(4).or(secondIndex)
        result[i.shr(1)] = octet.toByte()
    }

    return result
}

fun String.toBooleanExtra(): Boolean {
    return this.toBoolean() || this == "1"
}

fun String.prepareFilename(replaceWith: String = ""): String {
    return this
        .map { ch -> if (FORBIDDEN_CHARS.any { ch == it } ) "_" else ch }
        .joinToString(separator = replaceWith)
}