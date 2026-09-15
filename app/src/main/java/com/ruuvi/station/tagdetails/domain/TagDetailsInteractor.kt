package com.ruuvi.station.tagdetails.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.history.HistoryRange
import com.ruuvi.station.history.HistorySampler
import com.ruuvi.station.history.SampledHistory
import com.ruuvi.station.units.model.UnitType
import kotlinx.coroutines.currentCoroutineContext
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.tag.domain.RuuviTag
import kotlinx.coroutines.ensureActive
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
        return listOf(settings?.temperatureOffset, settings?.humidityOffset, settings?.pressureOffset)
    }

    suspend fun sampleHistory(
        sensorId: String, range: HistoryRange, units: List<UnitType>,
        value: (TagSensorReading, UnitType) -> Double?
    ): Map<UnitType, SampledHistory> {
        val context = currentCoroutineContext()
        val settings = sensorSettingsRepository.getSensorSettings(sensorId)
        val samplers = units.associateWith { HistorySampler(range) }
        sensorHistoryRepository.forEachReading(sensorId, range) { reading ->
            context.ensureActive()
            reading.temperature = reading.temperature?.plus(settings?.temperatureOffset ?: 0.0)
            reading.humidity = reading.humidity?.plus(settings?.humidityOffset ?: 0.0)
            reading.pressure = reading.pressure?.plus(settings?.pressureOffset ?: 0.0)
            for ((unit, sampler) in samplers) sampler.add(reading.createdAt.time, value(reading, unit))
        }
        return samplers.mapValues { it.value.finish() }
    }
}
