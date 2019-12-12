package com.ruuvi.station.bluetooth.gateway.impl

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.ruuvi.station.bluetooth.gateway.ScannerServiceBluetoothGateway
import com.ruuvi.station.bluetooth.gateway.ScannerServiceBluetoothGateway.DiscoveredRuuviTagListener
import com.ruuvi.station.model.NeovisionariesLeScanResult
import com.ruuvi.station.util.Utils

class ScannerServiceAndroidBluetoothGateway(private val application: Application) : ScannerServiceBluetoothGateway {

    var foreground: Boolean = false

    private val TAG = ScannerServiceAndroidBluetoothGateway::class.java.simpleName

    private var scanning: Boolean = false
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var scanSettings: ScanSettings? = null
    private var scanner: BluetoothLeScanner? = null

    private lateinit var scanCallback: ScanCallback

    init {
        scanSettings = ScanSettings.Builder()
            .setReportDelay(0)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val bluetoothManager =
            application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothAdapter = bluetoothManager.adapter
        scanner = bluetoothAdapter?.bluetoothLeScanner
    }

    override fun startScan(discoveredTagListener: DiscoveredRuuviTagListener) {
        if (scanning || !canScan()) return
        scanning = true
        try {
            scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    super.onScanResult(callbackType, result)
                    result.scanRecord?.let { scanRecord ->
                        foundDevice(
                            result.device,
                            result.rssi,
                            scanRecord.bytes,
                            discoveredTagListener
                        )
                    }
                }
            }
            scanner?.startScan(Utils.getScanFilters(), scanSettings, scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, e.message)
            scanning = false
            Toast.makeText(application, "Couldn't start scanning, is bluetooth disabled?", Toast.LENGTH_LONG).show()
        }
    }

    override fun stopScan() {
        if (!canScan()) return
        scanning = false
        scanner?.stopScan(scanCallback)
    }

    private fun foundDevice(
        device: BluetoothDevice,
        rssi: Int,
        data: ByteArray,
        discoveredTagListener: DiscoveredRuuviTagListener
    ) {
        val dev = NeovisionariesLeScanResult()
        dev.device = device
        dev.rssi = rssi
        dev.scanData = data
        //Log.d(TAG, "found: " + device.getAddress());
        val tag = dev.parse(application)
        if (tag != null) discoveredTagListener.tagFound(tag, foreground)
    }

    override fun canScan(): Boolean {
        return scanner != null
    }
}