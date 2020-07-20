package com.ruuvi.station.bluetooth.domain

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.util.Foreground
import timber.log.Timber

class BluetoothStateWatcher(private val interactor: BluetoothInteractor) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_OFF -> Timber.d("Bluetooth off")
                BluetoothAdapter.STATE_TURNING_OFF -> Timber.d("Turning Bluetooth off...")
                BluetoothAdapter.STATE_TURNING_ON -> Timber.d("Turning Bluetooth on...")
                BluetoothAdapter.STATE_ON -> {
                    Timber.d("Bluetooth on")
                    interactor.restoreBluetoothScan()
                    if (Foreground.get().isForeground) interactor.startForegroundScanning()
                }
            }
        }
    }
}