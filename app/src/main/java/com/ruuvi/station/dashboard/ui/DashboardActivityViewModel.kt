package com.ruuvi.station.dashboard.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.bluetooth.BluetoothInteractor

class DashboardActivityViewModel(
    private val bluetoothInteractor: BluetoothInteractor
) : ViewModel() {

    fun startForegroundScanning() {
        if (bluetoothInteractor.canScan())
            bluetoothInteractor.startForegroundScanning()
    }
}