package com.ruuvi.station.bluetooth

import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.contract.FoundRuuviTag
import com.ruuvi.station.bluetooth.contract.IRuuviTagScanner
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.database.tables.isAir
import com.ruuvi.station.dataforwarding.domain.DataForwardingSender
import com.ruuvi.station.util.extensions.logData
import com.ruuvi.station.widgets.update.WidgetRefreshScheduler
import android.content.Context
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import java.util.HashMap

@Suppress("NAME_SHADOWING")
class DefaultOnTagFoundListener(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val dataForwardingSender: DataForwardingSender,
    private val repository: TagRepository,
    private val alarmCheckInteractor: AlarmCheckInteractor,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val sensorHistoryRepository: SensorHistoryRepository
) : IRuuviTagScanner.OnTagFoundListener {

    var isForeground = false

    private var lastLogged: MutableMap<String, Long> = HashMap()
    private var lastCleanedDate: Long = Date().time
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onTagFound(tag: FoundRuuviTag) {
        Timber.d("onTagFound: ${tag.logData()}")
        saveReading(RuuviTagEntity(tag))
        cleanUpOldData()
    }

    private fun saveReading(ruuviTag: RuuviTagEntity) {
        Timber.d("saveReading for tag(${ruuviTag.id})")
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Timber.d("Error while saving advertisement: ${throwable.message}")
        }
        (ioScope + coroutineExceptionHandler).launch {
            ruuviTag.id?.let { sensorId ->
                if (shouldDropAdvertisement(ruuviTag)) return@launch
                val dbTag = repository.getTagById(sensorId)
                if (dbTag != null) {
                    dbTag.preserveData(ruuviTag)
                    val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
                    if (!shouldSkipForCloudMode(sensorSettings)) {
                        repository.updateTag(ruuviTag)
                        if (sensorSettings != null && ruuviTag.dataFormat !in legacyAirDataformats) {
                            saveFavoriteReading(ruuviTag, sensorSettings)
                        }
                        sensorSettings?.let {
                            alarmCheckInteractor.checkAlarmsForSensor(ruuviTag, sensorSettings)
                        }
                    }
                } else {
                    ruuviTag.updateAt = Date()
                    repository.saveTag(ruuviTag)
                }
            }
        }
    }

    private fun saveFavoriteReading(ruuviTag: RuuviTagEntity, sensorSettings: SensorSettings) {
        ruuviTag.id?.let { sensorId ->
            if (shouldSaveReading(sensorId)) {
                Timber.d("saveFavoriteReading actual SAVING for ${ruuviTag.id}")
                val reading = TagSensorReading(ruuviTag)
                reading.save()
                dataForwardingSender.sendData(ruuviTag, sensorSettings)
                WidgetRefreshScheduler.enqueueSimpleRefreshForSensor(context, sensorId)
                WidgetRefreshScheduler.enqueueComplexRefreshForSensor(context, sensorId)
            } else {
                Timber.d("saveFavoriteReading SKIPPED ${ruuviTag.id}")
            }
        }
    }

    private fun shouldSaveReading(sensorId: String): Boolean {
        val interval = if (isForeground) {
            DATA_LOG_INTERVAL
        } else {
            preferencesRepository.getBackgroundScanInterval()
        }
        Timber.d("saveFavoriteReading (interval = $interval)")
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, -interval)
        val loggingThreshold = calendar.time.time
        val lastLoggedDate = lastLogged[sensorId]
        val shouldSave = lastLoggedDate == null || lastLoggedDate < loggingThreshold
        if (shouldSave) lastLogged[sensorId] = Date().time
        return shouldSave
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

    private fun shouldSkipForCloudMode(sensorSettings: SensorSettings?): Boolean {
        return preferencesRepository.signedIn() &&
            preferencesRepository.isCloudModeEnabled() &&
            sensorSettings?.networkSensor == true &&
            sensorSettings.networkLastSync != null
    }

    private fun shouldDropAdvertisement(ruuviTag: RuuviTagEntity): Boolean {
        val rssiFail = ruuviTag.rssi > 0
        val pmFail = ruuviTag.isAir() && ruuviTag.pm1 != null && ruuviTag.pm25 != null
                && ruuviTag.pm4 != null && ruuviTag.pm10 != null && (
                        (ruuviTag.pm1 ?: 0.0) > (ruuviTag.pm25 ?: 0.0) ||
                        (ruuviTag.pm25 ?: 0.0) > (ruuviTag.pm4 ?: 0.0) ||
                        (ruuviTag.pm4 ?: 0.0) > (ruuviTag.pm10 ?: 0.0))
        return rssiFail || pmFail
    }

    companion object {
        private const val DATA_LOG_INTERVAL = 0
        val legacyAirDataformats = listOf(0xF0, 0x06)
    }
}