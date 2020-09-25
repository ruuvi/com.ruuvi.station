package com.ruuvi.station.dashboard.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

class DashboardActivityViewModel(
    private val tagInteractor: TagInteractor,
    val converter: UnitsConverter
) : ViewModel() {

    private val tags = MutableLiveData<List<RuuviTag>>(arrayListOf())
    val observeTags: LiveData<List<RuuviTag>> = tags

    private val getTagsTimer = Timer("DashboardActivityViewModelTimer", false)

    init {
        getTagsFlow()
    }

    private fun getTagsFlow() {
        getTagsTimer.scheduleAtFixedRate(0, 1000) {
            viewModelScope.launch {
                val getTags = tagInteractor.getTags()
                withContext(Dispatchers.Main) {
                    tags.value = getTags
                }
            }
        }
    }
}