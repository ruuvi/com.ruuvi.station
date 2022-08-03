package com.ruuvi.station.tagsettings.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.units.model.TemperatureUnit

class TagSettingsInteractor(
    private val tagRepository: TagRepository,
    private val preferencesRepository: PreferencesRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val networkInteractor: RuuviNetworkInteractor
    ) {
    fun getTagById(tagId: String): RuuviTagEntity? =
        tagRepository
            .getTagById(tagId)

    fun updateTag(tag: RuuviTagEntity) =
        tagRepository.updateTag(tag)

    fun getTemperatureUnit(): TemperatureUnit =
        preferencesRepository.getTemperatureUnit()

    fun deleteTagsAndRelatives(tag: RuuviTagEntity) {
        val sensorId = tag.id.toString()
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        tagRepository.deleteSensorAndRelatives(tag)
        sensorSettings?.owner?.let { owner ->
            if (sensorSettings.owner == networkInteractor.getEmail()) {
                networkInteractor.unclaimSensor(sensorId)
            } else {
                networkInteractor.unshareSensor(networkInteractor.getEmail() ?: "", sensorId)
            }
        }
    }

    fun updateTagName(sensorId: String, sensorName: String?) =
        sensorSettingsRepository.updateSensorName(sensorId, sensorName)

    fun updateTagBackground(tagId: String, userBackground: String?, defaultBackground: Int?) =
        sensorSettingsRepository.updateSensorBackground(tagId, userBackground, defaultBackground, null)

    fun updateNetworkBackground(tagId: String, guid: String?) {
        sensorSettingsRepository.updateNetworkBackground(tagId, guid)
    }

    fun getSensorSettings(sensorId: String): SensorSettings? = sensorSettingsRepository.getSensorSettings(sensorId)

    fun setSensorFirmware(sensorId: String, firmware: String) = sensorSettingsRepository.setSensorFirmware(sensorId, firmware)

    fun checkSensorOwner(sensorId: String) {
        networkInteractor.checkSensorOwner(sensorId)
    }
}