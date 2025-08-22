package com.ruuvi.station.util.extensions

import com.ruuvi.station.bluetooth.contract.FoundRuuviTag

fun FoundRuuviTag.logData(): String {
    return "tag[$id]: dataFormat = $dataFormat; temp = $temperature; humidity = $humidity; pressure = $pressure"
}