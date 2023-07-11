package com.ruuvi.station.tagdetails.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
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

    fun getTagReadings(sensorId: String): List<TagSensorReading> {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        val history =  if (preferences.isShowAllGraphPoint()) {
            sensorHistoryRepository.getHistory(sensorId, preferences.getGraphViewPeriodDays())
        } else {
            sensorHistoryRepository.getCompositeHistory(sensorId, preferences.getGraphViewPeriodDays(), preferences.getGraphPointInterval())
        }.map { it.copy(
            temperature = it.temperature + (sensorSettings?.temperatureOffset ?: 0.0),
            humidity = it.humidity?.let { humidity -> humidity + (sensorSettings?.humidityOffset ?: 0.0)},
            pressure = it.pressure?.let { pressure -> pressure + (sensorSettings?.pressureOffset ?: 0.0)}
        )
        }
        return history
    }
}