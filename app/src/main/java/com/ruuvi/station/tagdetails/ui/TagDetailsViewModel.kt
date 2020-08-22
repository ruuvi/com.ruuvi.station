package com.ruuvi.station.tagdetails.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.alarm.domain.AlarmStatus
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.tagdetails.domain.TagDetailsArguments
import com.ruuvi.station.util.BackgroundScanModes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

@ExperimentalCoroutinesApi
class TagDetailsViewModel(
    tagDetailsArguments: TagDetailsArguments,
    private val interactor: TagInteractor,
    private val alarmCheckInteractor: AlarmCheckInteractor
) : ViewModel() {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var timer = Timer("timer", true)

    private val isShowGraph = MutableStateFlow<Boolean>(false)
    val isShowGraphFlow: StateFlow<Boolean> = isShowGraph

    private val selectedTag = MutableStateFlow<RuuviTag?>(null)
    val selectedTagFlow: StateFlow<RuuviTag?> = selectedTag

    private val tags = MutableStateFlow<List<RuuviTag>>(arrayListOf())
    val tagsFlow: StateFlow<List<RuuviTag>> = tags

    private val alarmStatus = MutableStateFlow(AlarmStatus.NO_ALARM)
    val alarmStatusFlow: StateFlow<AlarmStatus> = alarmStatus

    var dashboardEnabled = isDashboardEnabled()
    var tag: RuuviTag? = null

    var openAddView: Boolean = tagDetailsArguments.shouldOpenAddView
    var desiredTag: String? = tagDetailsArguments.desiredTag

    init {
        ioScope.launch {
            timer.scheduleAtFixedRate(0, 1000) {
                checkForAlarm()
            }
        }
    }

    fun pageSelected(pageIndex: Int) {
        viewModelScope.launch {
            selectedTag.value = tags.value[pageIndex]
            desiredTag = tags.value[pageIndex].id
            checkForAlarm()
        }
    }

    fun switchShowGraphChannel() {
        isShowGraph.value = !isShowGraph.value
    }

    fun refreshTags() {
        ioScope.launch {
            timer.scheduleAtFixedRate(0, 1000) {
                tags.value = interactor.getTags()
            }
        }
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
                val tagEntry = interactor.getTagByID(tagId)
                tagEntry?.let {
                    withContext(Dispatchers.Main) {
                        alarmStatus.value = alarmCheckInteractor.getStatus(it)
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