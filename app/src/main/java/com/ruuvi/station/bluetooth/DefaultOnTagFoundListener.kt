package com.ruuvi.station.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.gateway.GatewaySender
import com.ruuvi.station.model.HumidityCalibration
import com.ruuvi.station.util.extensions.logData
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import java.util.HashMap

@Suppress("NAME_SHADOWING")
class DefaultOnTagFoundListener(
    private val context: Context,
    private val preferences: Preferences,
    private val gatewaySender: GatewaySender,
    private val repository: TagRepository,
    private val alarmCheckInteractor: AlarmCheckInteractor
) : IRuuviTagScanner.OnTagFoundListener {

    var isForeground = false

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var lastLogged: MutableMap<String, Long> = HashMap()
    private var lastCleanedDate: Long = Date().time

    override fun onTagFound(tag: FoundRuuviTag) {
        Timber.d("onTagFound: ${tag.logData()}")
        updateLocation()
        val tag = HumidityCalibration.apply(RuuviTagEntity(tag))
        saveReading(tag)

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, -10)
        val cleaningThreshold = calendar.time.time
        if (lastCleanedDate == null || lastCleanedDate < cleaningThreshold) {
            Timber.d("Cleaning DB from old tag readings")
            TagSensorReading.removeOlderThan(72)
            lastCleanedDate = Date().time
        }
    }

    private fun saveReading(ruuviTag: RuuviTagEntity) {
        Timber.d("saveReading for tag(${ruuviTag.id})")
        val dbTag = ruuviTag.id?.let { repository.getTagById(it) }
        if (dbTag != null) {
            val ruuviTag = dbTag.preserveData(ruuviTag)
            repository.updateTag(ruuviTag)
            if (dbTag.favorite == true) saveFavouriteReading(ruuviTag)
        } else {
            ruuviTag.updateAt = Date()
            repository.saveTag(ruuviTag)
        }
    }

    private fun saveFavouriteReading(ruuviTag: RuuviTagEntity) {
        val interval = if (isForeground) {
            DATA_LOG_INTERVAL
        } else {
            preferences.backgroundScanInterval
        }
        Timber.d("saveFavouriteReading (interval = $interval)")
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, -interval)
        val loggingThreshold = calendar.time.time
        val lastLoggedDate = lastLogged[ruuviTag.id]
        if (lastLoggedDate == null || lastLoggedDate <= loggingThreshold) {
            ruuviTag.id?.let {
                Timber.d("saveFavouriteReading actual SAVING for [${ruuviTag.name}] (${ruuviTag.id})")
                lastLogged[it] = Date().time
                val reading = TagSensorReading(ruuviTag)
                reading.save()
                gatewaySender.sendData(ruuviTag, tagLocation)
            }
        } else {
            Timber.d("saveFavouriteReading SKIPPED [${ruuviTag.name}] (${ruuviTag.id}) lastLogged = ${Date(lastLoggedDate)}")
        }
        alarmCheckInteractor.check(ruuviTag)
    }

    private fun updateLocation() {
        if (
            preferences.gatewayUrl.isNotEmpty() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            Timber.d("updateLocation")
            fusedLocationClient.lastLocation.addOnSuccessListener { location -> tagLocation = location }
        }
    }

    companion object {
        private const val DATA_LOG_INTERVAL = 0

        var tagLocation: Location? = null
    }
}