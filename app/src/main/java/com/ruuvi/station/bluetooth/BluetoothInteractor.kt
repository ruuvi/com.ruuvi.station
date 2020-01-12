package com.ruuvi.station.bluetooth

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.Handler
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.raizlabs.android.dbflow.config.FlowManager
import com.ruuvi.station.bluetooth.gateway.BluetoothTagGateway
import com.ruuvi.station.service.AltBeaconScannerForegroundService
import com.ruuvi.station.service.RuuviRangeNotifier
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.ServiceUtils
import com.ruuvi.station.util.Utils
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import org.altbeacon.bluetooth.BluetoothMedic
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

    // NEW REFACTORING -------------------------------

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
            val su = ServiceUtils(application)
            disposeStuff()
            su.startForegroundService()
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
            Utils.setAltBeaconParsers(beaconManager)
            beaconManager!!.backgroundScanPeriod = 5000
            beaconManager!!.bind(consumer!!)
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
    // NEW REFACOTRING ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

//    fun stopScanning() {
//        bluetoothRangeGateway.stopScanning()
//    }
//
//    fun startForegroundScanning() {
//        if (runForegroundIfEnabled()) return
//        if (foreground) return
//        foreground = true
//        Utils.removeStateFile(application.applicationContext)
//        Log.d(TAG, "Starting foreground scanning")
//
//        gatewayOn = false
//
//        bindRangeGateway()
//
//        bluetoothRangeGateway.setBackgroundMode(false)
//    }
//
//    fun startBackgroundScanning() {
//        Log.d(TAG, "Starting background scanning")
//        if (runForegroundIfEnabled()) return
//        if (prefs?.backgroundScanMode != BackgroundScanModes.BACKGROUND) {
//            Log.d(TAG, "Background scanning is not enabled, ignoring")
//            return
//        }
//
//        gatewayOn = true
//
//        bindRangeGateway()
//
//        bluetoothRangeGateway.startBackgroundScanning()
//    }
//
//    fun onAppCreated() {
//
//        prefs = Preferences(application.applicationContext)
//
//        val listener: Foreground.Listener = object : Foreground.Listener {
//            override fun onBecameForeground() {
//                Log.d(TAG, "onBecameForeground")
//                startForegroundScanning()
//            }
//
//            override fun onBecameBackground() {
//                Log.d(TAG, "onBecameBackground")
//                foreground = false
//                val su = ServiceUtils(application.applicationContext)
//                if (prefs?.backgroundScanMode === BackgroundScanModes.DISABLED) { // background scanning is disabled so all scanning things will be killed
//                    stopScanning()
//                    su.stopForegroundService()
//                } else if (prefs?.backgroundScanMode === BackgroundScanModes.BACKGROUND) {
//                    if (su.isRunning(AltBeaconScannerForegroundService::class.java)) {
//                        su.stopForegroundService()
//                    } else {
//                        startBackgroundScanning()
//                    }
//                } else {
//                    disposeStuff()
//                    su.startForegroundService()
//                }
//            }
//        }
//
//        Foreground.init(application)
//        Foreground.get().addListener(listener)
//        Handler().postDelayed(
//            {
//                if (!foreground) {
//                    if (prefs?.backgroundScanMode == BackgroundScanModes.FOREGROUND) {
//                        ServiceUtils(application.applicationContext).startForegroundService()
//                    } else if (prefs?.backgroundScanMode == BackgroundScanModes.BACKGROUND) {
//                        startBackgroundScanning()
//                    }
//                }
//            },
//            5000
//        )
//    }
//
//    fun onCreateForegroundScanningService() {
//        bluetoothRangeGateway.reset()
//    }
//
//    fun onDestroyForegroundScannerService() {
//        bluetoothRangeGateway.reset()
//    }
//
//    fun startInBackgroundMode() {
//        bluetoothRangeGateway.startBackgroundScanning()
//    }
//
//    /* Is called right after startInBackgroundMode */
//    fun setBackgroundMode(isBackgroundModeEnabled: Boolean) {
//
//        bluetoothRangeGateway.setBackgroundMode(isBackgroundModeEnabled)
//        gatewayOn = isBackgroundModeEnabled
//    }
//
//    fun getBackgroundBetweenScanInterval(): Long? =
//        bluetoothRangeGateway.getBackgroundBetweenScanInterval()
//
//    fun isBluetoothEnabled(): Boolean {
//        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
//    }
//
//    fun checkAndEnableBluetooth(activity: Activity): Boolean {
//        if (isBluetoothEnabled()) {
//            return true
//        }
//
//        enableBluetooth(activity)
//        return false
//    }
//
//    fun checkAndEnableBluetoothForStarter(that: Activity): Boolean {
//        if (isBluetoothEnabled()) {
//            return true
//        }
//        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//        that.startActivityForResult(enableBtIntent, 87)
//        return false
//    }
//
//    private fun enableBluetooth(activity: Activity) {
//        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//        activity.startActivity(enableBtIntent)
//    }
//
//    private fun disposeStuff() {
//        bluetoothRangeGateway.reset()
//    }
//
//    private fun runForegroundIfEnabled(): Boolean {
//        if (prefs?.backgroundScanMode == BackgroundScanModes.FOREGROUND) {
//            val su = ServiceUtils(application.applicationContext)
//            disposeStuff()
//            su.startForegroundService()
//            return true
//        }
//        return false
//    }
//
//    private fun updateLocation() {
//        if (ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
//            && ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            fusedLocationClient.lastLocation.addOnSuccessListener { location -> tagLocation = location }
//        }
//    }
//
//    private fun bindRangeGateway() {
//
//        bluetoothRangeGateway.listenForTags(
//            object : BluetoothTagGateway.OnTagsFoundListener  {
//
//                override fun onFoundTags(tags: List<RuuviTag>) {
//
//                    if (gatewayOn) updateLocation()
//
//                    // save all tags
//                    for (tag in tags) {
//                        saveReading(tag)
//                    }
//
//                    val tagsToSend = tags.filter { it.favorite }
//                    // send favorite tags
//                    if (tagsToSend.isNotEmpty() && gatewayOn) Http.post(tagsToSend, tagLocation, application)
//
//                    TagSensorReading.removeOlderThan(24)
//                }
//            }
//        )
//    }
//
//    private fun saveReading(ruuviTag: RuuviTag) {
//
//        var ruuviTag = ruuviTag
//
//        val dbTag = RuuviTag.get(ruuviTag.id)
//
//        if (dbTag != null) {
//            ruuviTag = dbTag.preserveData(ruuviTag)
//            ruuviTag.update()
//            if (!dbTag.favorite) return
//        } else {
//            ruuviTag.updateAt = Date()
//            ruuviTag.save()
//            return
//        }
//
//        val calendar = Calendar.getInstance()
//        calendar.add(Calendar.SECOND, -Constants.DATA_LOG_INTERVAL)
//        val loggingThreshold = calendar.time.time
//        for ((key, value) in lastLogged.entries) {
//            if (key == ruuviTag.id && value > loggingThreshold) {
//                return
//            }
//        }
//        lastLogged[ruuviTag.id] = Date().time
//        val reading = TagSensorReading(ruuviTag)
//        reading.save()
//        AlarmChecker.check(ruuviTag, application)
//    }

    companion object {
        var foreground = false
    }
}