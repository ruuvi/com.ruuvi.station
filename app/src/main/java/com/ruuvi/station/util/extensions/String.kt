package com.ruuvi.station.util.extensions

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.util.Locale

private const val HEX_CHARS = "0123456789ABCDEF"
private val FORBIDDEN_CHARS = arrayOf('"', '*', '/', ':', '<', '>', '?', '\\', '|')

fun String.hexStringToByteArray(): ByteArray {
    val input = this.uppercase(Locale.getDefault())

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

fun String.loadList(): List<String> {
    val listType = object : TypeToken<List<String>>() {}.type
    return if (this.isEmpty() || this == "null") {
        emptyList()
    } else {
        try {
            Gson().fromJson(this, listType)
        } catch (exception: JsonSyntaxException) {
            emptyList()
        }
    }
}