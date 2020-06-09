package com.ruuvi.station.startup.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.bluetooth.BluetoothInteractor

class StartupActivityViewModel(
    private val bluetoothInteractor: BluetoothInteractor,
    val preferences: Preferences
) : ViewModel() {
    fun startForegroundScanning() {
        if (bluetoothInteractor.canScan())
            bluetoothInteractor.startForegroundScanning()
    }
}