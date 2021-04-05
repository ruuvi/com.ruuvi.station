package com.ruuvi.station.bluetooth.domain

import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.IRuuviGattListener
import java.util.*

class BluetoothGattInteractor (
        private val interactor: BluetoothInteractor
) {
    fun readLogs(id: String, from: Date?, listener: IRuuviGattListener): Boolean {
        return interactor.readLogs(id, from, listener)
    }
    fun disconnect(id: String): Boolean {
        return interactor.disconnect(id)
    }
}