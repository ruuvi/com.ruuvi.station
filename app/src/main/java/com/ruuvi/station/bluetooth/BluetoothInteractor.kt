package com.ruuvi.station.bluetooth

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.Handler
import android.util.Log
import com.raizlabs.android.dbflow.config.FlowManager
import com.ruuvi.station.bluetooth.gateway.BluetoothTagGateway
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.service.AltBeaconScannerForegroundService
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Constants
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.ServiceUtils
import com.ruuvi.station.util.Utils
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.Region
import org.altbeacon.bluetooth.BluetoothMedic
import java.util.ArrayList
import java.util.HashMap

class BluetoothInteractor(private val application: Application) : BeaconConsumer {

//    val isForegroundScanningActive: Boolean
//        get() = bluetoothRangeGateway.isForegroundScanningActive()

    private val TAG: String = BluetoothInteractor::class.java.simpleName

    private val prefs: Preferences = Preferences(application)

    private val bluetoothRangeGateway = BluetoothTagGateway()

    private var tagLocation: Location? = null

    private var lastLogged: MutableMap<String, Long> = HashMap()

//    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)

    private var gatewayOn: Boolean = false

    private val backgroundTags: List<RuuviTag> = ArrayList()

    private var beaconManager: BeaconManager? = null
    private var region: Region? = null
    var running = false
    private var ruuviRangeNotifier: RuuviRangeNotifier? = null
    private var foreground = false
    var medic: BluetoothMedic? = null

    fun stopScanning() {
        Log.d(TAG, "Stopping scanning")
        running = false
        try {
            beaconManager!!.stopRangingBeaconsInRegion(region!!)
        } catch (e: Exception) {
            Log.d(TAG, "Could not remove ranging region")
        }
    }

    fun disposeStuff() {
        Log.d(TAG, "Stopping scanning")
        medic = null
        if (beaconManager == null) return
        running = false
        beaconManager!!.removeRangeNotifier(ruuviRangeNotifier!!)
        try {
            beaconManager!!.stopRangingBeaconsInRegion(region!!)
        } catch (e: Exception) {
            Log.d(TAG, "Could not remove ranging region")
        }
        beaconManager!!.unbind(this)
        beaconManager = null
    }

    private fun runForegroundIfEnabled(): Boolean {
        if (prefs.backgroundScanMode === BackgroundScanModes.FOREGROUND) {
            val serviceUtils = ServiceUtils(application)
            disposeStuff()
            serviceUtils.startForegroundService()
            return true
        }
        return false
    }

    fun startForegroundScanning() {
        if (runForegroundIfEnabled()) return
        if (foreground) return
        foreground = true
        Utils.removeStateFile(application)
        Log.d(TAG, "Starting foreground scanning")
        bindBeaconManager(this, application)
        beaconManager!!.backgroundMode = false
        if (ruuviRangeNotifier != null) ruuviRangeNotifier!!.gatewayOn = false
    }

    fun startBackgroundScanning() {
        Log.d(TAG, "Starting background scanning")
        if (runForegroundIfEnabled()) return
        if (prefs.backgroundScanMode !== BackgroundScanModes.BACKGROUND) {
            Log.d(TAG, "Background scanning is not enabled, ignoring")
            return
        }
        bindBeaconManager(this, application)
        var scanInterval = Preferences(application).backgroundScanInterval * 1000
        val minInterval = 15 * 60 * 1000
        if (scanInterval < minInterval) scanInterval = minInterval
        if (scanInterval.toLong() != beaconManager!!.backgroundBetweenScanPeriod) {
            beaconManager!!.backgroundBetweenScanPeriod = scanInterval.toLong()
            try {
                beaconManager!!.updateScanPeriods()
            } catch (e: Exception) {
                Log.e(TAG, "Could not update scan intervals")
            }
        }
        beaconManager!!.backgroundMode = true
        if (ruuviRangeNotifier != null) ruuviRangeNotifier!!.gatewayOn = true
        if (medic == null) medic = setupMedic(application)
    }

    fun setupMedic(context: Context?): BluetoothMedic? {
        val medic = BluetoothMedic.getInstance()
        medic.enablePowerCycleOnFailures(context)
        medic.enablePeriodicTests(context, BluetoothMedic.SCAN_TEST)
        return medic
    }

    private fun bindBeaconManager(consumer: BeaconConsumer?, context: Context) {
        if (beaconManager == null) {
            beaconManager = BeaconManager.getInstanceForApplication(context.applicationContext)

            beaconManager?.let {
                setAltBeaconParsers(it)
                it.backgroundScanPeriod = 5000
                it.bind(consumer!!)
            }
        } else if (!running) {
            running = true
            try {
                beaconManager!!.startRangingBeaconsInRegion(region!!)
            } catch (e: Exception) {
                Log.d(TAG, "Could not start ranging again")
            }
        }
    }

    fun onCreate() {
        Log.d(TAG, "App class onCreate")
        FlowManager.init(application)
        ruuviRangeNotifier = RuuviRangeNotifier(application, "RuuviScannerApplication")
        Foreground.init(application)
        Foreground.get().addListener(listener)
        Handler().postDelayed({
            if (!foreground) {
                if (prefs.backgroundScanMode === BackgroundScanModes.FOREGROUND) {
                    ServiceUtils(application).startForegroundService()
                } else if (prefs.backgroundScanMode === BackgroundScanModes.BACKGROUND) {
                    startBackgroundScanning()
                }
            }
        }, 5000)
        region = Region("com.ruuvi.station.leRegion", null, null, null)
    }

    var listener: Foreground.Listener = object : Foreground.Listener {
        override fun onBecameForeground() {
            Log.d(TAG, "onBecameForeground")
            startForegroundScanning()
            if (ruuviRangeNotifier != null) ruuviRangeNotifier!!.gatewayOn = false
        }

        override fun onBecameBackground() {
            Log.d(TAG, "onBecameBackground")
            foreground = false
            val su = ServiceUtils(application)
            if (prefs.backgroundScanMode === BackgroundScanModes.DISABLED) { // background scanning is disabled so all scanning things will be killed
                stopScanning()
                su.stopForegroundService()
            } else if (prefs.backgroundScanMode === BackgroundScanModes.BACKGROUND) {
                if (su.isRunning(AltBeaconScannerForegroundService::class.java)) {
                    su.stopForegroundService()
                } else {
                    startBackgroundScanning()
                }
            } else {
                disposeStuff()
                su.startForegroundService()
            }
            if (ruuviRangeNotifier != null) ruuviRangeNotifier!!.gatewayOn = true
        }
    }

    override fun getApplicationContext(): Context {
        return application
    }

    override fun unbindService(p0: ServiceConnection?) {
        application.unbindService(p0)
    }

    override fun bindService(p0: Intent?, p1: ServiceConnection?, p2: Int): Boolean {
        return application.bindService(p0, p1, p2)
    }

    override fun onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect")
        //Toast.makeText(application, "Started scanning (Application)", Toast.LENGTH_SHORT).show();
        ruuviRangeNotifier!!.gatewayOn = !foreground
        if (!beaconManager!!.rangingNotifiers.contains(ruuviRangeNotifier)) {
            beaconManager!!.addRangeNotifier(ruuviRangeNotifier!!)
        }
        running = true
        try {
            beaconManager!!.startRangingBeaconsInRegion(region!!)
        } catch (e: Exception) {
            Log.e(TAG, "Could not start ranging")
        }
    }

    companion object {
        var foreground = false

        fun setAltBeaconParsers(beaconManager: BeaconManager) {
            beaconManager.beaconParsers.clear()
            beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(Constants.RuuviV2and4_LAYOUT))
            val v3Parser = BeaconParser().setBeaconLayout(Constants.RuuviV3_LAYOUT)
            v3Parser.setHardwareAssistManufacturerCodes(intArrayOf(1177))
            beaconManager.beaconParsers.add(v3Parser)
            val v5Parser = BeaconParser().setBeaconLayout(Constants.RuuviV5_LAYOUT)
            v5Parser.setHardwareAssistManufacturerCodes(intArrayOf(1177))
            beaconManager.beaconParsers.add(v5Parser)
        }
    }
}