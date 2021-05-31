package com.ruuvi.station.bluetooth

import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.domain.LocationInteractor
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.gateway.GatewaySender
import com.ruuvi.station.util.extensions.logData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import java.util.HashMap

@Suppress("NAME_SHADOWING")
class DefaultOnTagFoundListener(
    private val preferencesRepository: PreferencesRepository,
    private val gatewaySender: GatewaySender,
    private val repository: TagRepository,
    private val alarmCheckInteractor: AlarmCheckInteractor,
    private val locationInteractor: LocationInteractor,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val sensorHistoryRepository: SensorHistoryRepository
) : IRuuviTagScanner.OnTagFoundListener {

    var isForeground = false

    private var lastLogged: MutableMap<String, Long> = HashMap()
    private var lastCleanedDate: Long = Date().time
    private var locationUpdateDate: Long = Long.MIN_VALUE
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onTagFound(tag: FoundRuuviTag) {
        Timber.d("onTagFound: ${tag.logData()}")
        updateLocation()
        saveReading(RuuviTagEntity(tag))
        cleanUpOldData()
    }

    private fun saveReading(ruuviTag: RuuviTagEntity) {
        Timber.d("saveReading for tag(${ruuviTag.id})")
        ioScope.launch {
            ruuviTag.id?.let { sensorId ->
                val dbTag = repository.getTagById(sensorId)
                if (dbTag != null) {
                    val ruuviTag = dbTag.preserveData(ruuviTag)
                    val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
                    sensorSettings?.calibrateSensor(ruuviTag)
                    repository.updateTag(ruuviTag)
                    if (dbTag.favorite == true && sensorSettings != null) saveFavoriteReading(ruuviTag, sensorSettings)
                } else {
                    ruuviTag.updateAt = Date()
                    repository.saveTag(ruuviTag)
                }
            }
        }
    }

    private fun saveFavoriteReading(ruuviTag: RuuviTagEntity, sensorSettings: SensorSettings) {
        val interval = if (isForeground) {
            DATA_LOG_INTERVAL
        } else {
            preferencesRepository.getBackgroundScanInterval()
        }
        Timber.d("saveFavoriteReading (interval = $interval)")
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, -interval)
        val loggingThreshold = calendar.time.time
        val lastLoggedDate = lastLogged[ruuviTag.id]
        if (lastLoggedDate == null || lastLoggedDate <= loggingThreshold) {
            ruuviTag.id?.let {
                Timber.d("saveFavoriteReading actual SAVING for ${ruuviTag.id}")
                lastLogged[it] = Date().time
                val reading = TagSensorReading(ruuviTag)
                reading.save()
                gatewaySender.sendData(ruuviTag, locationInteractor.lastLocation)
            }
        } else {
            Timber.d("saveFavoriteReading SKIPPED ${ruuviTag.id} lastLogged = ${Date(lastLoggedDate)}")
        }
        alarmCheckInteractor.check(ruuviTag, sensorSettings)
    }

    private fun cleanUpOldData() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, -10)
        val cleaningThreshold = calendar.time.time
        if (lastCleanedDate < cleaningThreshold) {
            Timber.d("Cleaning DB from old tag readings")
            sensorHistoryRepository.removeOlderThan(GlobalSettings.historyLengthHours)
            lastCleanedDate = Date().time
        }
    }

    private fun updateLocation() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, -1)
        val cleaningThreshold = calendar.time.time
        if (locationUpdateDate < cleaningThreshold) {
            locationInteractor.updateLocation()
            locationUpdateDate = Date().time
        }
    }

    companion object {
        private const val DATA_LOG_INTERVAL = 0
    }
}