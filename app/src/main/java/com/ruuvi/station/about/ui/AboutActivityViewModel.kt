package com.ruuvi.station.about.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.tag.domain.TagInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
class AboutActivityViewModel(
    private val tagInteractor: TagInteractor
) : ViewModel() {

    private val tagsSizes = MutableStateFlow<Pair<Int, Int>?>(null)
    val tagsSizesFlow: StateFlow<Pair<Int, Int>?> = tagsSizes

    init {
        CoroutineScope(Dispatchers.IO)
            .launch { tagsSizes.value = Pair(getAllFavouriteTagsSize(), getAllNonFavouriteTagsSize()) }
    }

    private suspend fun getAllFavouriteTagsSize(): Int {
        return suspendCoroutine { it.resume(tagInteractor.getTagEntities(true).size) }
    }

    private suspend fun getAllNonFavouriteTagsSize(): Int {
        return suspendCoroutine { it.resume(tagInteractor.getTagEntities(false).size) }
    }

    fun getHistoryLength(): Long = tagInteractor.getHistoryLength()
}