package com.ruuvi.station.tagdetails.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

class TagDetailsViewModel(
    tagDetailsArguments: TagDetailsArguments,
    private val interactor: TagInteractor,
    private val alarmCheckInteractor: AlarmCheckInteractor
) : ViewModel() {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var timer = Timer("timer", true)

    private val isShowGraph = MutableLiveData<Boolean>(false)
    val isShowGraphObserve: LiveData<Boolean> = isShowGraph

    private val selectedTag = MutableLiveData<RuuviTag?>(null)
    val selectedTagObserve: LiveData<RuuviTag?> = selectedTag

    private val tags = MutableLiveData<List<RuuviTag>>(arrayListOf())
    val tagsObserve: LiveData<List<RuuviTag>> = tags

    private val alarmStatus = MutableLiveData(AlarmStatus.NO_ALARM)
    val alarmStatusObserve: LiveData<AlarmStatus> = alarmStatus

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
            selectedTag.value = tags.value?.get(pageIndex)
            desiredTag = tags.value?.get(pageIndex)?.id
            checkForAlarm()
        }
    }

    fun switchShowGraphChannel() {
        isShowGraph.value = !(isShowGraph.value ?: true)
    }

    fun refreshTags() {
        ioScope.launch {
            val list = interactor.getTags()
            withContext(Dispatchers.Main) {
                    tags.value = list
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