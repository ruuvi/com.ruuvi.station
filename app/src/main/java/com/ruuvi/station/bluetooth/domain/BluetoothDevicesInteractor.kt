package com.ruuvi.station.bluetooth.domain

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.ruuvi.station.bluetooth.model.DeviceInfo
import timber.log.Timber

class BluetoothDevicesInteractor(
    private val context: Context
){
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private var discoveredCallback: ((DeviceInfo) -> Unit)? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        Timber.d("ACTION_FOUND ${device.address}, ${device.name}")
                        discoveredCallback?.invoke(DeviceInfo(device.address, device.name))
                    }
                }
            }
        }
    }

    init {
        context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    fun discoverDevices(discoveredCallback: (DeviceInfo) -> Unit) {
        this.discoveredCallback = discoveredCallback
        bluetoothAdapter.startDiscovery()
    }

    fun cancelDiscovery() {
        bluetoothAdapter.cancelDiscovery()
    }
}