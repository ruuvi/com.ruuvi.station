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

    fun getTagReadings(tagId: String): List<TagSensorReading> {
        return if (preferences.isShowAllGraphPoint()) {
            sensorHistoryRepository.getHistory(tagId, preferences.getGraphViewPeriodDays())
        } else {
            sensorHistoryRepository.getCompositeHistory(tagId, preferences.getGraphViewPeriodDays(), preferences.getGraphPointInterval())
        }
    }

    fun getTemperatureString(tag: RuuviTag): String =
        unitsConverter.getTemperatureString(tag.temperature)

    fun getTemperatureStringWithoutUnit(tag: RuuviTag): String =
        unitsConverter.getTemperatureStringWithoutUnit(tag.temperature)

    fun getTemperatureUnitString(): String =
        unitsConverter.getTemperatureUnitString()

    fun getHumidityString(tag: RuuviTag): String =
        unitsConverter.getHumidityString(tag.humidity, tag.temperature)

    fun getPressureString(tag: RuuviTag): String =
        unitsConverter.getPressureString(tag.pressure)

    fun getSignalString(tag: RuuviTag): String =
        unitsConverter.getSignalString(tag.rssi)

    fun getViewPeriod(): Int = preferences.getGraphViewPeriodDays()

    fun setViewPeriod(periodDays: Int) {
        preferences.setGraphViewPeriodDays(periodDays)
    }
}