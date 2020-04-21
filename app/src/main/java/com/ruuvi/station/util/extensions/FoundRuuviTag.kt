package com.ruuvi.station.util.extensions

import com.ruuvi.station.bluetooth.FoundRuuviTag

fun FoundRuuviTag.logData() : String {
    return "tag[$id]: dataFormat = $dataFormat; temp = $temperature; humidity = $humidity; pressure = $pressure"
}