package com.ruuvi.station.startup.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.startup.domain.StartupActivityInteractor

class StartupActivityViewModel(
    private val bluetoothInteractor: BluetoothInteractor,
    private val startupInteractor: StartupActivityInteractor,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    fun startForegroundScanning() {
        if (bluetoothInteractor.canScan())
            bluetoothInteractor.startForegroundScanning()
    }

    fun isFirstStart(): Boolean = startupInteractor.isFirstStart()

    fun isDashboardEnabled(): Boolean = startupInteractor.isDashboardEnabled()

    fun generateDeviceId() {
        preferencesRepository.getDeviceId()
    }
}