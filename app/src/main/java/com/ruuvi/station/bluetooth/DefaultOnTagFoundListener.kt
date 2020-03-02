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
import com.ruuvi.station.util.Preferences
import java.util.Calendar
import java.util.Date
import java.util.HashMap

class DefaultOnTagFoundListener(val context: Context) : IRuuviTagScanner.OnTagFoundListener {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var lastLogged: MutableMap<String, Long> = HashMap()

    override fun onTagFound(foundTag: FoundRuuviTag) {
        updateLocation()
        val favoriteTags = ArrayList<RuuviTagEntity>()
        val tag = HumidityCalibration.apply(RuuviTagEntity(foundTag))
        saveReading(tag)
        if (tag.favorite == true) {
            favoriteTags.add(tag)
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
            if (dbTag.favorite == true) saveFavouriteReading(ruuviTag)
        } else {
            ruuviTag.updateAt = Date()
            RuuviTagRepository.save(ruuviTag)
        }
    }

    private fun saveFavouriteReading(ruuviTag: RuuviTagEntity) {
        val interval = Preferences(context).backgroundScanInterval
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, -interval)
        val loggingThreshold = calendar.time.time
        var lastLoggedDate = lastLogged[ruuviTag.id]
        if (lastLoggedDate == null || lastLoggedDate <= loggingThreshold) {
            ruuviTag.id?.let {
                lastLogged[it] = Date().time
                val reading = TagSensorReading(ruuviTag)
                reading.save()
            }
        }
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