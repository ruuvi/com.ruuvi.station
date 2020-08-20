package com.ruuvi.station.tagdetails.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.alarm.AlarmCheckInteractor
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.util.BackgroundScanModes
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

@ExperimentalCoroutinesApi
class TagDetailsViewModel(
    private val interactor: TagInteractor
) : ViewModel() {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var timer = Timer("timer", true)

    private val isShowGraph = MutableStateFlow<Boolean>(false)
    val isShowGraphFlow: StateFlow<Boolean> = isShowGraph

    private val selectedTag = MutableStateFlow<RuuviTagEntity?>(null)
    val selectedTagFlow: StateFlow<RuuviTagEntity?> = selectedTag

    //FIXME change livedata to coroutines
    val tags = MutableLiveData<List<RuuviTagEntity>>()

    private val alarmStatus = MutableStateFlow(-1)
    val alarmStatusFlow: StateFlow<Int> = alarmStatus

    var dashboardEnabled = isDashboardEnabled()
    var tag: RuuviTagEntity? = null

    init {
        ioScope.launch {
            timer.scheduleAtFixedRate(0, 1000) {
                checkForAlarm()
            }
        }
    }

    fun pageSelected(pageIndex: Int) {
        viewModelScope.launch {
            tags.value?.let {
                selectedTag.value = it[pageIndex]
            }
            checkForAlarm()
        }
    }

    fun switchShowGraphChannel() {
        isShowGraph.value = !isShowGraph.value
    }

    fun refreshTags() {
        tags.value = interactor.getTagEntities()
    }

    fun getBackgroundScanMode(): BackgroundScanModes =
        interactor.getBackgroundScanMode()

    fun setBackgroundScanMode(mode: BackgroundScanModes) =
        interactor.setBackgroundScanMode(mode)

    fun isFirstGraphVisit(): Boolean =
        interactor.isFirstGraphVisit()

    fun setIsFirstGraphVisit(isFirst: Boolean) =
        interactor.setIsFirstGraphVisit(isFirst)

    private fun isDashboardEnabled(): Boolean =
        interactor.isDashboardEnabled()

    private fun checkForAlarm() {
        ioScope.launch {
            selectedTag.value?.id?.let { tagId ->
                val tagEntry = interactor.getTagEntityById(tagId)
                tagEntry?.let {
                    withContext(Dispatchers.Main) {
                        alarmStatus.value = AlarmCheckInteractor.getStatus(it)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        timer.cancel()
        super.onCleared()
    }
}