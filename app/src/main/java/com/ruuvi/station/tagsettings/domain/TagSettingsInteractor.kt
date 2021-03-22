package com.ruuvi.station.tagsettings.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.units.model.TemperatureUnit

class TagSettingsInteractor(
    private val tagRepository: TagRepository,
    private val preferencesRepository: PreferencesRepository
) {
    fun getTagById(tagId: String): RuuviTagEntity? =
        tagRepository
            .getTagById(tagId)

    fun updateTag(tag: RuuviTagEntity) =
        tagRepository.updateTag(tag)

    fun getTemperatureUnit(): TemperatureUnit =
        preferencesRepository.getTemperatureUnit()

    fun deleteTagsAndRelatives(tag: RuuviTagEntity) =
        tagRepository.deleteTagsAndRelatives(tag)

    fun updateTagName(tagId: String, tagName: String?) =
        tagRepository.updateTagName(tagId, tagName)

    fun updateTagBackground(tagId: String, userBackground: String?, defaultBackground: Int?) =
        tagRepository.updateTagBackground(tagId, userBackground, defaultBackground, null)

    fun updateNetworkBackground(tagId: String, guid: String?) {
        tagRepository.updateNetworkBackground(tagId, guid)
    }
}