package com.ruuvi.station.bluetooth

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.RemoteException
import android.support.v4.content.ContextCompat
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ruuvi.station.bluetooth.interfaces.IRuuviTag
import com.ruuvi.station.bluetooth.interfaces.RuuviTagFactory
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.RangeNotifier
import org.altbeacon.beacon.Region
import org.altbeacon.bluetooth.BluetoothMedic
import java.util.ArrayList
import java.util.HashMap

class RuuviRangeNotifier(
    private val context: Context,
    private val ruuviTagFactory: RuuviTagFactory,
    private val from: String
) : RangeNotifier {

    private var tagListener: OnTagsFoundListener? = null

    private val medic: BluetoothMedic = setupMedic(context)
    private val region = Region("com.ruuvi.station.leRegion", null, null, null)
    private val beaconManager = BeaconManager.getInstanceForApplication(context)
    private val lastLogged: MutableMap<String, Long> = HashMap()

    private val beaconConsumer = object : BeaconConsumer {

        override fun getApplicationContext(): Context = context

        override fun unbindService(p0: ServiceConnection?) {
            context.unbindService(p0)
        }

        override fun bindService(p0: Intent?, p1: ServiceConnection?, p2: Int): Boolean {
            return context.bindService(p0, p1, p2)
        }

        override fun onBeaconServiceConnect() {
            Log.d(TAG, "onBeaconServiceConnect")

            startRanging()
        }
    }
    private val mFusedLocationClient: FusedLocationProviderClient
    private var last: Long = 0

    private fun updateLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.lastLocation.addOnSuccessListener { location -> tagLocation = location }
        }
    }

    init {
        Log.d(TAG, "Setting up range notifier from $from")
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun startScan(tagListener: OnTagsFoundListener) {
        this.tagListener = tagListener
        setAltBeaconParsers(beaconManager)
        beaconManager.backgroundScanPeriod = 5000

        beaconManager.bind(beaconConsumer)
    }

    fun setAltBeaconParsers(beaconManager: BeaconManager) {
        beaconManager.beaconParsers.clear()
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(RuuviV2and4_LAYOUT))
        val v3Parser = BeaconParser().setBeaconLayout(RuuviV3_LAYOUT)
        v3Parser.setHardwareAssistManufacturerCodes(intArrayOf(1177))
        beaconManager.beaconParsers.add(v3Parser)
        val v5Parser = BeaconParser().setBeaconLayout(RuuviV5_LAYOUT)
        v5Parser.setHardwareAssistManufacturerCodes(intArrayOf(1177))
        beaconManager.beaconParsers.add(v5Parser)
    }

    private fun startRanging() {

        if (!beaconManager.rangingNotifiers.contains(this)) {
            beaconManager.addRangeNotifier(this)
        }
        try {
            beaconManager.startRangingBeaconsInRegion(region!!)
        } catch (e: RemoteException) {
            Log.e(TAG, "Could not start ranging")
        }
    }

    private fun setupMedic(context: Context?): BluetoothMedic {
        val medic = BluetoothMedic.getInstance()
        medic.enablePowerCycleOnFailures(context)
        medic.enablePeriodicTests(context, BluetoothMedic.SCAN_TEST)
        return medic
    }

    override fun didRangeBeaconsInRegion(beacons: Collection<Beacon>, region: Region) {
        val now = System.currentTimeMillis()
        if (now <= last + 500) {
            Log.d(TAG, "Double range bug")
            return
        }
        last = now

        updateLocation()

        val tags: MutableList<IRuuviTag> = ArrayList()
        val allTags: MutableList<IRuuviTag> = ArrayList()
        Log.d(TAG, from + " " + " found " + beacons.size)
        for (beacon in beacons) { // the same tag can appear multiple times
            for (tag in tags) {
                if (tag.id == beacon.bluetoothAddress) continue
            }
            val tag = LeScanResult.fromAltbeacon(context, ruuviTagFactory, beacon)
            if (tag != null) {
                allTags.add(tag)
//                saveReading(tag)
                if (tag.favorite) tags.add(tag)
            }
        }
//        if (tags.size > 0 && gatewayOn) Http.post(tags, tagLocation, context)
//        TagSensorReading.removeOlderThan(24)

        tagListener?.onFoundTags(allTags = allTags)
    }

    fun stopScanning() {

        beaconManager.removeRangeNotifier(this)
        try {
            beaconManager.stopRangingBeaconsInRegion(region)
        } catch (e: Exception) {
            Log.d(TAG, "Could not stop ranging region")
        }
        beaconManager.unbind(beaconConsumer)
    }

    fun enableBackgroundMode(isBackgroundModeEnabled: Boolean) {
        beaconManager.backgroundMode = isBackgroundModeEnabled
    }

    fun getBackgroundScanInterval(): Long = beaconManager.backgroundBetweenScanPeriod

    fun setEnableScheduledScanJobs(areScheduledScanJobsEnabled: Boolean) {
        beaconManager.setEnableScheduledScanJobs(areScheduledScanJobsEnabled)
    }

    fun setBackgroundScanInterval(scanInterval: Long) {
        beaconManager.backgroundBetweenScanPeriod = scanInterval
        try {
            beaconManager.updateScanPeriods()
        } catch (e: Exception) {
            Log.e(TAG, "Could not update scan intervals")
        }
    }

    fun addTagListener(onTagsFoundListener: OnTagsFoundListener) {
        tagListener = onTagsFoundListener
    }

    companion object {
        private const val TAG = "RuuviRangeNotifier"
        // FIXME: remove tagLocation from static
        var tagLocation: Location? = null
        // FIXME: remove gateway on from static

        const val RuuviV2and4_LAYOUT = "s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-21v"
        //const val RuuviV3_LAYOUT = "x,m:0-1=9904,m:2-2=03,i:3-15,d:3-3,d:4-4,d:5-5,d:6-7,d:8-9,d:10-11,d:12-13,d:14-15";
        //const val RuuviV5_LAYOUT = "x,m:0-1=9904,m:2-2=05,i:20-25,d:3-4,d:5-6,d:7-8,d:9-10,d:11-12,d:13-14,d:15-16,d:17-17,d:18-19,d:20-25";
        const val RuuviV3_LAYOUT = "x,m:0-2=990403,i:2-15,d:2-2,d:3-3,d:4-4,d:5-5,d:6-6,d:7-7,d:8-8,d:9-9,d:10-10,d:11-11,d:12-12,d:13-13,d:14-14,d:15-15"
        const val RuuviV5_LAYOUT = "x,m:0-2=990405,i:20-25,d:2-2,d:3-3,d:4-4,d:5-5,d:6-6,d:7-7,d:8-8,d:9-9,d:10-10,d:11-11,d:12-12,d:13-13,d:14-14,d:15-15,d:16-16,d:17-17,d:18-18,d:19-19,d:20-20,d:21-21,d:22-22,d:23-23,d:24-24,d:25-25"
        const val DATA_LOG_INTERVAL = 5
    }

    interface OnTagsFoundListener {
        fun onFoundTags(allTags: List<IRuuviTag>)
    }
}