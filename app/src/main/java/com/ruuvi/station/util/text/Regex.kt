package com.ruuvi.station.util.text

const val decimalsPattern = "\\-?\\d+[\\.,\\,]?\\d*"

fun String.getDecimalMatches() = Regex(decimalsPattern).findAll(this)