package com.ruuvi.station.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.support.v4.content.ContextCompat
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ruuvi.station.gateway.Http
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.model.TagSensorReading
import com.ruuvi.station.util.AlarmChecker
import com.ruuvi.station.util.Constants
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.RangeNotifier
import org.altbeacon.beacon.Region
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.HashMap

internal class RuuviRangeNotifier(context: Context, from: String) : RangeNotifier {

    private val from: String
    private val context: Context
    private var lastLogged: MutableMap<String, Long>? = null
    private val mFusedLocationClient: FusedLocationProviderClient
    private var last: Long = 0

    private fun updateLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.lastLocation.addOnSuccessListener { location -> tagLocation = location }
        }
    }

    init {
        Log.d(TAG, "Setting up range notifier from $from")
        this.context = context
        this.from = from
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    override fun didRangeBeaconsInRegion(beacons: Collection<Beacon>, region: Region) {
        val now = System.currentTimeMillis()
        if (now <= last + 500) {
            Log.d(TAG, "Double range bug")
            return
        }
        last = now
        if (gatewayOn) updateLocation()
        val tags: MutableList<RuuviTag> = ArrayList()
        Log.d(TAG, from + " " + " found " + beacons.size)
        foundBeacon@ for (beacon in beacons) { // the same tag can appear multiple times
            for (tag in tags) {
                if (tag.id == beacon.bluetoothAddress) continue@foundBeacon
            }
            val tag = LeScanResult.fromAltbeacon(context, beacon)
            if (tag != null) {
                saveReading(tag)
                if (tag.favorite) tags.add(tag)
            }
        }
        if (tags.size > 0 && gatewayOn) Http.post(tags, tagLocation, context)
        TagSensorReading.removeOlderThan(24)
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
        if (lastLogged == null) lastLogged = HashMap()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, -Constants.DATA_LOG_INTERVAL)
        val loggingThreshold = calendar.time.time
        for ((key, value) in lastLogged!!) {
            if (key == ruuviTag.id && value > loggingThreshold) {
                return
            }
        }
        lastLogged!![ruuviTag.id] = Date().time
        val reading = TagSensorReading(ruuviTag)
        reading.save()
        AlarmChecker.check(ruuviTag, context)
    }


    companion object {
        private const val TAG = "RuuviRangeNotifier"
        // FIXME: remove tagLocation from static
        var tagLocation: Location? = null
        // FIXME: remove gateway on from static
        var gatewayOn = false
    }

    interface OnTagsFoundListener {
        fun onFoundTags(allTags: List<RuuviTag>)
    }

}