package com.ruuvi.station.tag.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.util.BackgroundScanModes

class TagInteractor(
    private val tagRepository: TagRepository,
    private val preferencesRepository: PreferencesRepository,
    private val converter: TagConverter
) {

    fun getTags(isFavorite: Boolean = true): List<RuuviTag> =
        tagRepository
            .getAllTags(isFavorite)
            .map { converter.fromDatabase(it) }

    fun getTagEntities(isFavorite: Boolean = true): List<RuuviTagEntity> =
        tagRepository.getAllTags(isFavorite)

    fun getTagEntityById(tagId: String): RuuviTagEntity? =
        tagRepository.getTagById(tagId)

    fun getBackgroundScanMode(): BackgroundScanModes =
        preferencesRepository.getBackgroundScanMode()

    fun setBackgroundScanMode(mode: BackgroundScanModes) =
        preferencesRepository.setBackgroundScanMode(mode)

    fun isFirstGraphVisit(): Boolean =
        preferencesRepository.isFirstGraphVisit()

    fun setIsFirstGraphVisit(isFirst: Boolean) =
        preferencesRepository.setIsFirstGraphVisit(isFirst)

    fun isDashboardEnabled(): Boolean =
        preferencesRepository.isDashboardEnabled()
}