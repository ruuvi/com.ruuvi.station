package com.ruuvi.station.app.domain

import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor

class ImageMigrationInteractor(
    val sensorSettingsRepository: SensorSettingsRepository,
    val tagSettingsInteractor: TagSettingsInteractor,
) {

    fun migrateDefaultImages() {
        val settings = sensorSettingsRepository.getSensorSettings().filter {
            it.userBackground.isNullOrEmpty()
        }
        for (setting in settings) {
            tagSettingsInteractor.setDefaultBackgroundImage(setting.id, setting.defaultBackground)
        }
    }
}