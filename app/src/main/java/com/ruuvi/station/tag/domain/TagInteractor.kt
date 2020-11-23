package com.ruuvi.station.tag.domain

import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.util.BackgroundScanModes
import timber.log.Timber

class TagInteractor constructor(
    private val tagRepository: TagRepository,
    private val preferencesRepository: PreferencesRepository,
    private val converter: TagConverter,
    private val alarmCheckInteractor: AlarmCheckInteractor
) {

    fun getTags(isFavorite: Boolean = true): List<RuuviTag> =
        tagRepository
            .getAllTags(isFavorite)
            .map {
                val ruuviTag = converter.fromDatabase(it)
                val status = alarmCheckInteractor.getStatus(ruuviTag)
                ruuviTag.copy(status = status)
            }
            .also { Timber.d("TagInteractor - getTags") }

    fun getTagEntities(isFavorite: Boolean = true): List<RuuviTagEntity> =
        tagRepository.getAllTags(isFavorite)
            .also {Timber.d("TagInteractor - getTagEntities")}

    fun getTagEntityById(tagId: String): RuuviTagEntity? =
        tagRepository.getTagById(tagId)

    fun getTagByID(tagId: String): RuuviTag? =
        tagRepository
            .getTagById(tagId)
            ?.let { converter.fromDatabase(it) }

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