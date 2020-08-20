package com.ruuvi.station.tagsettings.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity

class TagSettingsInteractor(
    private val tagRepository: TagRepository,
    private val preferencesRepository: PreferencesRepository
) {

    fun getRepositoryInstance(): TagRepository =
        tagRepository

    fun getTagById(tagId: String): RuuviTagEntity? =
        tagRepository
            .getTagById(tagId)

    fun updateTag(tag: RuuviTagEntity) =
        tagRepository.updateTag(tag)

    fun getTemperatureUnit(): String =
        preferencesRepository.getTemperatureUnit()

    fun deleteTagsAndRelatives(tag: RuuviTagEntity) =
        tagRepository.deleteTagsAndRelatives(tag)
}