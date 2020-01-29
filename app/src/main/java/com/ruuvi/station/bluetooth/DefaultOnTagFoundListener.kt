package com.ruuvi.station.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.support.v4.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ruuvi.station.database.RuuviTagRepository
import com.ruuvi.station.gateway.Http
import com.ruuvi.station.model.HumidityCalibration
import com.ruuvi.station.model.RuuviTagEntity
import com.ruuvi.station.model.TagSensorReading
import com.ruuvi.station.util.AlarmChecker
import com.ruuvi.station.util.Constants
import java.util.Calendar
import java.util.Date
import java.util.HashMap

class DefaultOnTagFoundListener(val context: Context) : IRuuviRangeNotifier.OnTagsFoundListener {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var lastLogged: MutableMap<String, Long> = HashMap()

    override fun onTagsFound(allTags: List<FoundRuuviTag>) {

        updateLocation()

        val favoriteTags = ArrayList<RuuviTagEntity>()

        allTags.forEach {

            val tag = HumidityCalibration.apply(RuuviTagEntity(it))

            saveReading(tag)

            if (tag.favorite == true) {
                favoriteTags.add(tag)
            }
        }

        if (favoriteTags.size > 0 && gatewayOn) Http.post(favoriteTags, tagLocation, context)

        TagSensorReading.removeOlderThan(24)
    }

    private fun saveReading(ruuviTag: RuuviTagEntity) {
        var ruuviTag = ruuviTag
        val dbTag = RuuviTagRepository.get(ruuviTag.id)
        if (dbTag != null) {
            ruuviTag = dbTag.preserveData(ruuviTag)
            RuuviTagRepository.update(ruuviTag)
            if (dbTag.favorite!=true) return
        } else {
            ruuviTag.updateAt = Date()
            RuuviTagRepository.save(ruuviTag)
            return
        }
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, -Constants.DATA_LOG_INTERVAL)
        val loggingThreshold = calendar.time.time
        for ((key, value) in lastLogged!!) {
            if (key == ruuviTag.id && value > loggingThreshold) {
                return
            }
        }
        ruuviTag.id?.let { id ->
            lastLogged[id] = Date().time
        }
        val reading = TagSensorReading(ruuviTag)
        reading.save()
        AlarmChecker.check(ruuviTag, context)
    }

    private fun updateLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location -> tagLocation = location }
        }
    }

    companion object {
        var gatewayOn = false

        var tagLocation: Location? = null
    }
}