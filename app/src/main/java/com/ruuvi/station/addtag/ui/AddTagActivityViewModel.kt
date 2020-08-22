package com.ruuvi.station.addtag.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

@ExperimentalCoroutinesApi
class AddTagActivityViewModel(
    private val tagInteractor: TagInteractor
) : ViewModel() {

    private val timer = Timer("AddTagViewModelTimer", true)

    private val tags = MutableStateFlow<List<RuuviTagEntity>>(emptyList())
    val tagsFlow: StateFlow<List<RuuviTagEntity>> = tags

    init {
        CoroutineScope(Dispatchers.IO).launch {
            timer.scheduleAtFixedRate(0, 1000) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.SECOND, -5)

                tags.value =
                    getAllTags(false)
                        .mapNotNull { tag ->
                            if (tag.updateAt?.time?.compareTo(calendar.time.time) == -1) {
                                null
                            } else {
                                tag
                            }
                        }
                        .sortedByDescending { tag -> tag.rssi }
            }
        }
    }

    fun getTagById(tagId: String): RuuviTagEntity? =
        tagInteractor.getTagEntityById(tagId)

    fun getAllTags(isFavourite: Boolean): List<RuuviTagEntity> =
        tagInteractor.getTagEntities(isFavourite)
}