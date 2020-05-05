package com.ruuvi.station.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.support.v4.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ruuvi.station.database.RuuviTagRepository
import com.ruuvi.station.model.HumidityCalibration
import com.ruuvi.station.model.RuuviTagEntity
import com.ruuvi.station.model.TagSensorReading
import com.ruuvi.station.util.AlarmChecker
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.gateway.GatewaySender
import com.ruuvi.station.util.Constants
import com.ruuvi.station.util.extensions.logData
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import java.util.HashMap

class DefaultOnTagFoundListener(
        private val context: Context,
        private val preferences: Preferences,
        private val gatewaySender: GatewaySender
): IRuuviTagScanner.OnTagFoundListener {

    var isForeground = false

    private val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

    private var lastLogged: MutableMap<String, Long> = HashMap()

    override fun onTagFound(foundTag: FoundRuuviTag) {
        Timber.d("onTagFound: ${foundTag.logData()}")
        updateLocation()
        val tag = HumidityCalibration.apply(RuuviTagEntity(foundTag))
        saveReading(tag)
        TagSensorReading.removeOlderThan(24)
    }

    private fun saveReading(ruuviTag: RuuviTagEntity) {
        Timber.d("saveReading")
        var ruuviTag = ruuviTag
        val dbTag = RuuviTagRepository.get(ruuviTag.id)
        if (dbTag != null) {
            ruuviTag = dbTag.preserveData(ruuviTag)
            RuuviTagRepository.update(ruuviTag)
            if (dbTag.favorite == true) saveFavouriteReading(ruuviTag)
        } else {
            ruuviTag.updateAt = Date()
            RuuviTagRepository.save(ruuviTag)
        }
    }

    private fun saveFavouriteReading(ruuviTag: RuuviTagEntity) {
        val interval = if (isForeground) {
            Constants.DATA_LOG_INTERVAL
        } else {
            preferences.backgroundScanInterval
        }
        Timber.d("saveFavouriteReading (interval = $interval)")
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, -interval)
        val loggingThreshold = calendar.time.time
        var lastLoggedDate = lastLogged[ruuviTag.id]
        if (lastLoggedDate == null || lastLoggedDate <= loggingThreshold) {
            ruuviTag.id?.let {
                Timber.d("saveFavouriteReading actual saving")
                lastLogged[it] = Date().time
                val reading = TagSensorReading(ruuviTag)
                reading.save()
                gatewaySender.sendData(ruuviTag, tagLocation)
            }
        }
        AlarmChecker.check(ruuviTag, context)
    }

    private fun updateLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Timber.d("updateLocation")
            fusedLocationClient.lastLocation.addOnSuccessListener { location -> tagLocation = location }
        }
    }

    companion object {
        var tagLocation: Location? = null
    }
}