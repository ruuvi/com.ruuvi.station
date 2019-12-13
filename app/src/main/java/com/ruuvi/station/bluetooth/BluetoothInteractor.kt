package com.ruuvi.station.bluetooth

import android.Manifest
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ruuvi.station.RuuviScannerApplication
import com.ruuvi.station.bluetooth.gateway.BluetoothRangeGateway
import com.ruuvi.station.gateway.Http
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.model.TagSensorReading
import com.ruuvi.station.service.AltBeaconScannerForegroundService
import com.ruuvi.station.util.AlarmChecker
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Constants
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.ServiceUtils
import com.ruuvi.station.util.Utils
import java.util.Calendar
import java.util.Date
import java.util.HashMap

class BluetoothInteractor(private val application: Application) {

    val isForegroundScanningActive: Boolean
        get() = bluetoothRangeGateway.isForegroundScanningActive()

    private val TAG: String = BluetoothInteractor::class.java.simpleName

    private var prefs: Preferences? = null

    private val bluetoothRangeGateway = (application as RuuviScannerApplication).bluetoothRangeGatewayFactory.create()

    private var tagLocation: Location? = null

    private var lastLogged: MutableMap<String, Long> = HashMap()

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)

    private var gatewayOn: Boolean = false

    fun stopScanning() {
        bluetoothRangeGateway.stopScanning()
    }

    fun startForegroundScanning() {
        if (runForegroundIfEnabled()) return
        if (foreground) return
        foreground = true
        Utils.removeStateFile(application.applicationContext)
        Log.d(TAG, "Starting foreground scanning")

        gatewayOn = false

        bindRangeGateway()

        bluetoothRangeGateway.setBackgroundMode(false)
    }

    fun startBackgroundScanning() {
        Log.d(TAG, "Starting background scanning")
        if (runForegroundIfEnabled()) return
        if (prefs?.backgroundScanMode != BackgroundScanModes.BACKGROUND) {
            Log.d(TAG, "Background scanning is not enabled, ignoring")
            return
        }

        gatewayOn = true

        bindRangeGateway()

        bluetoothRangeGateway.startBackgroundScanning()
    }

    fun onAppCreated() {

        prefs = Preferences(application.applicationContext)

        val listener: Foreground.Listener = object : Foreground.Listener {
            override fun onBecameForeground() {
                Log.d(TAG, "onBecameForeground")
                startForegroundScanning()
            }

            override fun onBecameBackground() {
                Log.d(TAG, "onBecameBackground")
                foreground = false
                val su = ServiceUtils(application.applicationContext)
                if (prefs?.backgroundScanMode === BackgroundScanModes.DISABLED) { // background scanning is disabled so all scanning things will be killed
                    stopScanning()
                    su.stopForegroundService()
                } else if (prefs?.backgroundScanMode === BackgroundScanModes.BACKGROUND) {
                    if (su.isRunning(AltBeaconScannerForegroundService::class.java)) {
                        su.stopForegroundService()
                    } else {
                        startBackgroundScanning()
                    }
                } else {
                    disposeStuff()
                    su.startForegroundService()
                }
            }
        }

        Foreground.init(application)
        Foreground.get().addListener(listener)
        Handler().postDelayed(
            {
                if (!foreground) {
                    if (prefs?.backgroundScanMode == BackgroundScanModes.FOREGROUND) {
                        ServiceUtils(application.applicationContext).startForegroundService()
                    } else if (prefs?.backgroundScanMode == BackgroundScanModes.BACKGROUND) {
                        startBackgroundScanning()
                    }
                }
            },
            5000
        )
    }

    fun onCreateForegroundScanningService() {
        bluetoothRangeGateway.reset()
    }

    fun onDestroyForegroundScannerService() {
        bluetoothRangeGateway.reset()
    }

    fun startInBackgroundMode() {
        bluetoothRangeGateway.startBackgroundScanning()
    }

    /* Is called right after startInBackgroundMode */
    fun setBackgroundMode(isBackgroundModeEnabled: Boolean) {

        bluetoothRangeGateway.setBackgroundMode(isBackgroundModeEnabled)
        gatewayOn = isBackgroundModeEnabled
    }

    fun getBackgroundBetweenScanPeriod(): Long? =
        bluetoothRangeGateway.getBackgroundBetweenScanPeriod()

    fun setEnableScheduledScanJobs(areScheduledScanJobsEnabled: Boolean) {
        bluetoothRangeGateway.setEnableScheduledScanJobs(areScheduledScanJobsEnabled)
    }

    fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    fun checkAndEnableBluetooth(activity: Activity): Boolean {
        if (isBluetoothEnabled()) {
            return true
        }

        enableBluetooth(activity)
        return false
    }

    fun checkAndEnableBluetoothForStarter(that: Activity): Boolean {
        if (isBluetoothEnabled()) {
            return true
        }
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        that.startActivityForResult(enableBtIntent, 87)
        return false
    }

    private fun enableBluetooth(activity: Activity) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivity(enableBtIntent)
    }

    private fun disposeStuff() {
        bluetoothRangeGateway.reset()
    }

    private fun runForegroundIfEnabled(): Boolean {
        if (prefs?.backgroundScanMode == BackgroundScanModes.FOREGROUND) {
            val su = ServiceUtils(application.applicationContext)
            disposeStuff()
            su.startForegroundService()
            return true
        }
        return false
    }

    private fun updateLocation() {
        if (ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location -> tagLocation = location }
        }
    }

    private fun bindRangeGateway() {

        bluetoothRangeGateway.listenForRangeChanges(
            object : BluetoothRangeGateway.RangeListener {

                override fun onFoundTags(tags: List<RuuviTag>) {

                    if (gatewayOn) updateLocation()

                    // save all tags
                    for (tag in tags) {
                        saveReading(tag)
                    }

                    val tagsToSend = tags.filter { it.favorite }
                    // send favorite tags
                    if (tagsToSend.isNotEmpty() && gatewayOn) Http.post(tagsToSend, tagLocation, application)

                    TagSensorReading.removeOlderThan(24)
                }
            }
        )
    }

    private fun saveReading(ruuviTag: RuuviTag) {

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

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, -Constants.DATA_LOG_INTERVAL)
        val loggingThreshold = calendar.time.time
        for ((key, value) in lastLogged.entries) {
            if (key == ruuviTag.id && value > loggingThreshold) {
                return
            }
        }
        lastLogged[ruuviTag.id] = Date().time
        val reading = TagSensorReading(ruuviTag)
        reading.save()
        AlarmChecker.check(ruuviTag, application)
    }

    companion object {
        var foreground = false
    }
}