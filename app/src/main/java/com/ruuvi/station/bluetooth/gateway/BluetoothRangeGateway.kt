package com.ruuvi.station.bluetooth.gateway

interface BluetoothRangeGateway {

    fun listenForRangeChanges(rangeListener: RangeListener)

    interface RangeListener {

        fun onRangeChanged()
    }
}