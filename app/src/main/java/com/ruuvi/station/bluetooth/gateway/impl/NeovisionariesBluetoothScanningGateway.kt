package com.ruuvi.station.bluetooth.gateway.impl

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import com.ruuvi.station.RuuviScannerApplication
import com.ruuvi.station.bluetooth.gateway.BluetoothScanningGateway
import com.ruuvi.station.bluetooth.gateway.LeScanResultListener
import com.ruuvi.station.scanning.RuuviTagListener
import java.util.ArrayList

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
        scanner.startScan(getScanFilters(), scanSettings, scanCallback)
    }

    override fun startScan(listener: LeScanResultListener) {

        if (scanning || !canScan()) return
        scanning = true
        this.leScanResultListener = listener
        scanner.startScan(getScanFilters(), scanSettings, scanCallback)
    }

    override fun stopScan() {
        if (!canScan()) return
        scanning = false
        scanner.stopScan(scanCallback)
    }

    private fun getScanFilters(): List<ScanFilter>? {
        val filters: MutableList<ScanFilter> = ArrayList()
        val ruuviFilter = ScanFilter.Builder()
            .setManufacturerData(0x0499, byteArrayOf())
            .build()
        val eddystoneFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString("0000feaa-0000-1000-8000-00805f9b34fb"))
            .build()
        filters.add(ruuviFilter)
        filters.add(eddystoneFilter)
        return filters
    }

    private fun foundDevice(device: BluetoothDevice, rssi: Int, data: ByteArray) {
        val dev = (application as RuuviScannerApplication).leScanResultFactory
            .create(device.address, rssi, data)

        Log.d(TAG, "found: " + device.address)
        val tag = dev.parse(application)
        if (tag != null) tagListener?.tagFound(tag)
    }

    override fun canScan(): Boolean {
        return scanner != null
    }

    override fun listenForRangeChanges(rangeUniqueId: String, rangeListener: BluetoothScanningGateway.RangeListener) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
