package com.ruuvi.station.bluetooth.gateway.factory

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import com.ruuvi.station.model.NeovisionariesLeScanResult
import com.ruuvi.station.scanning.RuuviTagListener
import com.ruuvi.station.util.Utils

class NeovisionariesBluetoothScanningGateway(
    private val application: Application,
    bluetoothManager: BluetoothManager
) : BluetoothScanningGateway {

    private val TAG = NeovisionariesBluetoothScanningGateway::class.java.simpleName

    private val scanSettings: ScanSettings = ScanSettings.Builder()
        .setReportDelay(0)
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val bluetoothAdapter = bluetoothManager.adapter
    private val scanner = bluetoothAdapter.bluetoothLeScanner

    private var tagListener: RuuviTagListener? = null
    private var leScanResultListener: LeScanResultListener? = null

    private var scanning = false

    private val scanCallback: ScanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            result.scanRecord?.let {
                foundDevice(result.device, result.rssi, it.bytes)
            }
        }
    }

    override fun startScan(listener: RuuviTagListener) {

        if (scanning || !canScan()) return
        scanning = true
        this.tagListener = listener
        scanner.startScan(Utils.getScanFilters(), scanSettings, scanCallback)
    }

    override fun startScan(listener: LeScanResultListener) {

        if (scanning || !canScan()) return
        scanning = true
        this.leScanResultListener = listener
        scanner.startScan(Utils.getScanFilters(), scanSettings, scanCallback)
    }

    override fun stopScan() {
        if (!canScan()) return
        scanning = false
        scanner.stopScan(scanCallback)
    }

    private fun foundDevice(device: BluetoothDevice, rssi: Int, data: ByteArray) {
        val dev = NeovisionariesLeScanResult()
        dev.device = device
        dev.rssi = rssi
        dev.scanData = data
        Log.d(TAG, "found: " + device.address)
        val tag = dev.parse(application)
        if (tag != null) tagListener?.tagFound(tag)
    }

    override fun canScan(): Boolean {
        return scanner != null
    }
}
