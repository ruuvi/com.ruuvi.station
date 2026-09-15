package com.ruuvi.station.tagdetails.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.history.HistoryRange
import com.ruuvi.station.history.HistorySelection
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.domain.UnitsConverter

class TagDetailsInteractor(
    private val tagRepository: TagRepository,
    private val sensorHistoryRepository: SensorHistoryRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val unitsConverter: UnitsConverter,
    private val preferences: PreferencesRepository
) {

    fun getTagById(tagId: String): RuuviTag? =
        tagRepository.getFavoriteSensorById(tagId)

    fun clearLastSync(sensorId: String) =
        sensorSettingsRepository.clearLastSync(sensorId)

    fun readingOptions(sensorId: String): List<Any?> {
        val settings = sensorSettingsRepository.getSensorSettings(sensorId)
        return listOf(preferences.isShowAllGraphPoint(), preferences.getGraphPointInterval(),
            settings?.temperatureOffset, settings?.humidityOffset, settings?.pressureOffset)
    }

    fun getTagReadings(sensorId: String): List<TagSensorReading> {
        var viewPeriod = preferences.getGraphViewPeriodHours()
        return getTagReadings(sensorId, viewPeriod)
    }

    fun getTagReadings(sensorId: String, hours: Int): List<TagSensorReading> {
        return getTagReadings(sensorId, HistorySelection.Rolling(hours).resolve(System.currentTimeMillis()))
    }

    fun getTagReadings(sensorId: String, range: HistoryRange): List<TagSensorReading> {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        val history = if (preferences.isShowAllGraphPoint()) {
            sensorHistoryRepository.getHistory(sensorId, range)
        } else {
            sensorHistoryRepository.getCompositeHistory(sensorId, range, preferences.getGraphPointInterval())
        }.map { it.copy(
            temperature = it.temperature?.let { temperature -> temperature + (sensorSettings?.temperatureOffset ?: 0.0) },
            humidity = it.humidity?.let { humidity -> humidity + (sensorSettings?.humidityOffset ?: 0.0)},
            pressure = it.pressure?.let { pressure -> pressure + (sensorSettings?.pressureOffset ?: 0.0)}
        )
        }
        return history
    }
}