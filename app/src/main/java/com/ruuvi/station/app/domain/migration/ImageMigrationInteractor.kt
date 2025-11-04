package com.ruuvi.station.app.domain.migration

import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.image.ImageInteractor
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor

class ImageMigrationInteractor(
    val sensorSettingsRepository: SensorSettingsRepository,
    val tagSettingsInteractor: TagSettingsInteractor,
    val imageInteractor: ImageInteractor
) {

    fun migrateDefaultImages() {
        val settings = sensorSettingsRepository.getSensorSettings().filter {
            it.userBackground.isNullOrEmpty() && !it.networkSensor
        }
        for (setting in settings) {
            tagSettingsInteractor.setDefaultBackgroundImageByResource(
                sensorId = setting.id,
                defaultBackground = imageInteractor.getDefaultBackgroundById(setting.defaultBackground),
                uploadNow = false
            )
        }
    }
}