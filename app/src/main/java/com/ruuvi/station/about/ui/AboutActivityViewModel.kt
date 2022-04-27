package com.ruuvi.station.about.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.about.model.AppStats
import com.ruuvi.station.tag.domain.TagInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AboutActivityViewModel(
    private val tagInteractor: TagInteractor
) : ViewModel() {

    private val _appStats = MutableStateFlow<AppStats?>(null)
    val appStats = _appStats.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val favouriteTags = tagInteractor.getTagEntities(true).size
            val notFavouriteTags = tagInteractor.getTagEntities(false).size
            val measurementsCount = tagInteractor.getHistoryLength()

            _appStats.value = AppStats(
                favouriteTags = favouriteTags,
                seenTags = favouriteTags + notFavouriteTags,
                measurementsCount = measurementsCount
            )
        }
    }
}