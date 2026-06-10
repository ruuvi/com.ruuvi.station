package com.ruuvi.station.util

import java.text.Collator
import java.util.Comparator

class AlphabetNumberComparator : Comparator<String?> {
    private val collator = Collator.getInstance()

    private fun isDigit(ch: Char) = ch.isDigit()

    private fun getChunk(s: String, slen: Int, marker: Int): String {
        var currentMarker = marker
        val chunk = StringBuilder()
        var c = s[currentMarker]
        chunk.append(c)
        currentMarker++
        if (isDigit(c)) {
            while (currentMarker < slen) {
                c = s[currentMarker]
                if (!isDigit(c)) break
                chunk.append(c)
                currentMarker++
            }
        } else {
            while (currentMarker < slen) {
                c = s[currentMarker]
                if (isDigit(c)) break
                chunk.append(c)
                currentMarker++
            }
        }
        return chunk.toString()
    }

    override fun compare(s1: String?, s2: String?): Int {
        if (s1 == null && s2 == null) return 0
        if (s1 == null) return -1
        if (s2 == null) return 1

        var thisMarker = 0
        var thatMarker = 0
        val s1Length = s1.length
        val s2Length = s2.length

        while (thisMarker < s1Length && thatMarker < s2Length) {
            val thisChunk = getChunk(s1, s1Length, thisMarker)
            thisMarker += thisChunk.length
            val thatChunk = getChunk(s2, s2Length, thatMarker)
            thatMarker += thatChunk.length

            var result: Int
            if (isDigit(thisChunk[0]) && isDigit(thatChunk[0])) {
                try {
                    val thisValue = thisChunk.toLong()
                    val thatValue = thatChunk.toLong()
                    result = thisValue.compareTo(thatValue)
                } catch (_: NumberFormatException) {
                    result = thisChunk.compareTo(thatChunk)
                }
            } else {
                result = collator.compare(thisChunk, thatChunk)
            }

            if (result != 0) return result
        }

        return s1Length - s2Length
    }
}
