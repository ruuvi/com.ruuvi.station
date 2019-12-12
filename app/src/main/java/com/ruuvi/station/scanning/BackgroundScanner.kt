package com.ruuvi.station.scanning

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ruuvi.station.RuuviScannerApplication
import com.ruuvi.station.bluetooth.gateway.BackgroundBluetoothScannerGateway

class BackgroundScanner : BroadcastReceiver() {

    private lateinit var scannerGateway: BackgroundBluetoothScannerGateway

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Woke up")
        getScannerGateway(context).startScan()
    }

    private fun getScannerGateway(context: Context): BackgroundBluetoothScannerGateway {
        if (!::scannerGateway.isInitialized) {
            scannerGateway = (context.applicationContext as RuuviScannerApplication).scannerGatewayFactory.create()
        }

        return scannerGateway
    }

    companion object {
        const val REQUEST_CODE = 9001
        private val TAG = BackgroundScanner::class.java.simpleName
    }
}