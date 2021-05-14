package com.ruuvi.station.addtag.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.tag.domain.TagInteractor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

@ExperimentalCoroutinesApi
class AddTagActivityViewModel(
    private val tagInteractor: TagInteractor
) : ViewModel() {
    private val tags = MutableStateFlow<List<RuuviTagEntity>>(emptyList())
    val tagsFlow: StateFlow<List<RuuviTagEntity>> = tags

    fun updateTags() {
        CoroutineScope(Dispatchers.IO).launch {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.SECOND, -60)

            val tagsDb =
                getAllTags(false)
                    .mapNotNull { tag ->
                        if (tag.updateAt?.time?.compareTo(calendar.time.time) == -1) {
                            null
                        } else {
                            tag
                        }
                    }
                    .sortedByDescending { tag -> tag.rssi }
            withContext(Dispatchers.Main) {
                tags.value = tagsDb
            }
        }
    }

    fun getTagById(tagId: String): RuuviTagEntity? =
        tagInteractor.getTagEntityById(tagId)

    fun getAllTags(isFavourite: Boolean): List<RuuviTagEntity> =
        tagInteractor.getTagEntities(isFavourite)

    fun makeSensorFavorite(sensor: RuuviTagEntity) {
        tagInteractor.makeSensorFavorite(sensor)
    }
}