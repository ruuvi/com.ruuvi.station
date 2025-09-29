package com.ruuvi.station.tag.domain

import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dashboard.domain.SensorsSortingInteractor
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.isAir
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.MacAddressUtils
import timber.log.Timber
import java.util.Date

class TagInteractor constructor(
    private val tagRepository: TagRepository,
    private val sensorHistoryRepository: SensorHistoryRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val preferencesRepository: PreferencesRepository,
    private val alarmCheckInteractor: AlarmCheckInteractor,
    private val tagSettingsInteractor: TagSettingsInteractor,
    private val sortingInteractor: SensorsSortingInteractor,
) {

    fun getTags(): List<RuuviTag> =
        sortingInteractor.sortSensors(
            tagRepository
            .getFavoriteSensors()
            .map { it.copy(alarmSensorStatus = alarmCheckInteractor.getAlarmStatus(it)) }
            .also { Timber.d("TagInteractor - getTags") }
        )

    fun getTagEntities(isFavorite: Boolean = true): List<RuuviTagEntity> =
        tagRepository.getAllTags(isFavorite)
            .also {Timber.d("TagInteractor - getTagEntities")}

    fun getTagEntityById(tagId: String): RuuviTagEntity? =
        tagRepository.getTagById(tagId)

    fun getTagByID(tagId: String): RuuviTag? =
        tagRepository.getFavoriteSensorById(tagId)

    fun getBackgroundScanMode(): BackgroundScanModes =
        preferencesRepository.getBackgroundScanMode()

    fun setBackgroundScanMode(mode: BackgroundScanModes) {
        preferencesRepository.setBackgroundScanMode(mode)
    }

    fun isFirstGraphVisit(): Boolean =
        preferencesRepository.isFirstGraphVisit()

    fun setIsFirstGraphVisit(isFirst: Boolean) =
        preferencesRepository.setIsFirstGraphVisit(isFirst)

    fun getHistoryLength(): Long = sensorHistoryRepository.countAll()

    fun makeSensorFavorite(sensor: RuuviTagEntity) {
        sensor.id?.let { sensorId ->
            tagRepository.makeSensorFavorite(sensor)
            tagSettingsInteractor.setRandomDefaultBackgroundImage(sensorId)
            sensor.id?.let {sensorId ->
                sortingInteractor.addNewSensor(sensorId)
            }
        }
    }

    fun makeSensorFavorite(sensorId: String) {
        val existingSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        val tag = tagRepository.getTagById(sensorId)
        if (existingSettings == null) {
            val sensorSettings = SensorSettings(
                id = sensorId,
                createDate = Date(),
                name = MacAddressUtils.getDefaultName(sensorId, tag?.isAir())
            )
            sensorSettings.save()
            tagSettingsInteractor.setRandomDefaultBackgroundImage(sensorId)
            sortingInteractor.addNewSensor(sensorId)
        }
    }

    fun updateTagName(sensorId: String, sensorName: String?) {
        val tag = tagRepository.getTagById(sensorId)
        val name =
            if (sensorName.isNullOrEmpty()) MacAddressUtils.getDefaultName(sensorId, tag?.isAir()) else sensorName
        sensorSettingsRepository.updateSensorName(sensorId, name)
    }
}