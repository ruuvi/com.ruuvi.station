package com.ruuvi.station.bluetooth.gateway.impl

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.support.v4.content.ContextCompat
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.ruuvi.station.RuuviScannerApplication
import com.ruuvi.station.bluetooth.gateway.BackgroundBluetoothScannerGateway
import com.ruuvi.station.bluetooth.gateway.factory.BluetoothScanningGateway
import com.ruuvi.station.bluetooth.gateway.factory.LeScanResultListener
import com.ruuvi.station.bluetooth.model.LeScanResult
import com.ruuvi.station.gateway.Http
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.scanning.BackgroundScanner
import com.ruuvi.station.service.ScannerService
import com.ruuvi.station.util.Preferences
import java.util.ArrayList

class BackgroundAndroidBluetoothScannerGateway(private val application: Application) : BackgroundBluetoothScannerGateway {

    private val TAG = BackgroundAndroidBluetoothScannerGateway::class.java.simpleName
    private val SCAN_TIME_MS = 5000

    private var scanResults: MutableList<LeScanResult> = ArrayList()

    private var tagLocation: Location? = null

    private val bluetoothScanningGateway: BluetoothScanningGateway =
        (application as RuuviScannerApplication).bluetoothScanningGatewayFactory.create()

    override fun startScan() {

        scheduleNextScan(application)

        val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
        if (ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.lastLocation.addOnSuccessListener { location -> tagLocation = location }
        }

        if (!bluetoothScanningGateway.canScan()) {
            Log.d(TAG, "Could not start scanning in background, scheduling next attempt")
            return
        }

        bluetoothScanningGateway.startScan(
            object : LeScanResultListener {
                override fun onLeScanResult(result: LeScanResult) {

                    val scanResultIterator = scanResults.iterator()

                    var isDeviceAlreadyInScanResults = false

                    while (scanResultIterator.hasNext()) {
                        val element = scanResultIterator.next()
                        if (result.hasSameDevice(element)) isDeviceAlreadyInScanResults = true
                    }

                    if (!isDeviceAlreadyInScanResults) {
                        Log.d(TAG, "Found: $result")
                        scanResults.add(result)
                    }
                }
            }
        )

        Handler().postDelayed(
            {
                bluetoothScanningGateway.stopScan()
                processFoundDevices(application)
            },
            SCAN_TIME_MS.toLong()
        )
    }

    private fun processFoundDevices(context: Context?) {
        val tags: MutableList<RuuviTag> = ArrayList()
        val scanResultIterator = scanResults.iterator()
        while (scanResultIterator.hasNext()) {
            val element = scanResultIterator.next()
            val tag = element.parse(context)
            tag?.let { addFoundTagToLists(it, tags, context) }
        }
        Log.d(TAG, "Found " + tags.size + " tags")
        Http.post(tags, tagLocation, context)
        Log.d(TAG, "Going to sleep")
    }

    private fun scheduleNextScan(context: Context) {
        val prefs = Preferences(context)
        //int scanInterval = Integer.parseInt(settings.getString("pref_scaninterval", "30")) * 1000;
        var scanInterval = prefs.backgroundScanInterval * 1000
        if (scanInterval < 15 * 1000) scanInterval = 15 * 1000
        val batterySaving = prefs.batterySaverEnabled
        val intent = Intent(context, BackgroundScanner::class.java)
        val sender = PendingIntent.getBroadcast(context, BackgroundScanner.REQUEST_CODE, intent, 0)
        val am = context
            .getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!batterySaving) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + scanInterval, sender)
            } else {
                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + scanInterval, sender)
            }
        }
    }

    private fun checkForSameTag(arr: List<RuuviTag>, ruuvi: RuuviTag): Int {
        for (i in arr.indices) {
            if (ruuvi.id == arr[i].id) {
                return i
            }
        }
        return -1
    }

    fun addFoundTagToLists(tag: RuuviTag, tags: MutableList<RuuviTag>, context: Context?) {
        val index = checkForSameTag(tags, tag)
        if (index == -1) {
            tags.add(tag)
            ScannerService.logTag(tag, context, true)
        }
    }
}