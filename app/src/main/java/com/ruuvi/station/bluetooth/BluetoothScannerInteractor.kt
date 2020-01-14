package com.ruuvi.station.bluetooth

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.ruuvi.station.gateway.Http
import com.ruuvi.station.model.LeScanResult
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.model.TagSensorReading
import com.ruuvi.station.scanning.RuuviTagListener
import com.ruuvi.station.scanning.RuuviTagScanner
import com.ruuvi.station.util.AlarmChecker
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.Preferences
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.HashMap

class BluetoothScannerInteractor(private val application: Application) {

    private val TAG: String = BluetoothScannerInteractor::class.java.simpleName

    private val prefs: Preferences = Preferences(application)

    private var foreground: Boolean = true

    private val backgroundTags = ArrayList<RuuviTag>()

    //    private val bluetoothAdapter: BluetoothAdapter? = null
//    private val scanFilters: List<ScanFilter> = ArrayList()

    private val lastLogged: MutableMap<String, Long> = HashMap()
    private val LOG_INTERVAL = 5 // seconds

    private var scanning = false
//    private val scanSettings = ScanSettings.Builder()
//        .setReportDelay(0)
//        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//        .build()
//
//    val scanner by lazy {
//        val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        val bluetoothAdapter = bluetoothManager.getAdapter()
//        bluetoothAdapter.bluetoothLeScanner
//    }

    private val ruuviTagScanner by lazy {
        RuuviTagScanner(
            RuuviTagListener { logTag(it, application, foreground) },
            application
        )
    }

    init {

        val listener: Foreground.Listener = object : Foreground.Listener {
            override fun onBecameForeground() {
                foreground = true
            }

            override fun onBecameBackground() {
                foreground = false
            }
        }

        Foreground.init(application)
        Foreground.get().addListener(listener)
    }

    fun logTag(ruuviTag: RuuviTag, context: Context?, foreground: Boolean) {
        var ruuviTag = ruuviTag
        val dbTag = RuuviTag.get(ruuviTag.id)
        if (dbTag != null) {
            ruuviTag = dbTag.preserveData(ruuviTag)
            ruuviTag.update()
            if (!dbTag.favorite) return
        } else {
            ruuviTag.updateAt = Date()
            ruuviTag.save()
            return
        }
        if (!foreground) {
            if (ruuviTag.favorite && checkForSameTag(backgroundTags, ruuviTag) == -1) {
                backgroundTags.add(ruuviTag)
            }
            return
        }
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, -LOG_INTERVAL)
        val loggingThreshold = calendar.time.time
        for ((key, value) in lastLogged) {
            if (key == ruuviTag.id && value > loggingThreshold) {
                return
            }
        }
        val tags: MutableList<RuuviTag> = ArrayList()
        tags.add(ruuviTag)
        Http.post(tags, null, context)
        lastLogged[ruuviTag.id] = Date().time
        val reading = TagSensorReading(ruuviTag)
        reading.save()
        AlarmChecker.check(ruuviTag, context)
    }

    fun getBackgroundTags(): List<RuuviTag> = backgroundTags

    fun clearBackgroundTags() {
        backgroundTags.clear()
    }

    fun startScan() {
        if (scanning || !ruuviTagScanner.canScan()) return
        scanning = true
        try {
            ruuviTagScanner.start()
//            scanner.startScan(Utils.getScanFilters(), scanSettings, nsCallback)
        } catch (e: Exception) {
            Log.e(TAG, e.message)
            scanning = false
            Toast.makeText(application, "Couldn't start scanning, is bluetooth disabled?", Toast.LENGTH_LONG).show()
        }
    }

    fun stopScan() {
        if (!ruuviTagScanner.canScan()) return
        scanning = false
        ruuviTagScanner.stop()
//        scanner.stopScan(nsCallback)
    }

    private val nsCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            foundDevice(result.device, result.rssi, result.scanRecord.bytes)
        }
    }

    private fun foundDevice(device: BluetoothDevice, rssi: Int, data: ByteArray) {
        val dev = LeScanResult()
        dev.device = device
        dev.rssi = rssi
        dev.scanData = data
        //Log.d(TAG, "found: " + device.getAddress());
        val tag = dev.parse(application)
        if (tag != null) logTag(tag, application, foreground)
    }

//    private fun canScan(): Boolean {
//        return scanner != null
//    }

    private fun checkForSameTag(arr: List<RuuviTag>, ruuvi: RuuviTag): Int {
        for (i in arr.indices) {
            if (ruuvi.id == arr[i].id) {
                return i
            }
        }
        return -1
    }

}