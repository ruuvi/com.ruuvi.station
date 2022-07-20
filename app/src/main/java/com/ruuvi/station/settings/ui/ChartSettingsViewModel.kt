package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.SelectionElement
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChartSettingsViewModel(
    private val interactor: AppSettingsInteractor
) : ViewModel() {

    private val _viewPeriod =  MutableStateFlow<Int>(interactor.getGraphViewPeriod())
    val viewPeriod: StateFlow<Int> = _viewPeriod

    private val _showAllPoints = MutableStateFlow(interactor.isShowAllGraphPoint())
    val showAllPoints: StateFlow<Boolean> = _showAllPoints

    private val _drawDots = MutableStateFlow(interactor.graphDrawDots())
    val drawDots: StateFlow<Boolean> = _drawDots

    private var graphViewPeriodPreviousValue: Int = -1

    fun setViewPeriod(newPeriod: Int) {
        interactor.setGraphViewPeriod(newPeriod)
        _viewPeriod.value = interactor.getGraphViewPeriod()
    }

    fun setShowAllPoints(isChecked: Boolean) {
        interactor.setIsShowAllGraphPoint(isChecked)
        _showAllPoints.value = interactor.isShowAllGraphPoint()
    }

    fun setDrawDots(isChecked: Boolean) {
        interactor.setGraphDrawDots(isChecked)
        _drawDots.value = interactor.graphDrawDots()
    }

    fun getViewPeriodOptions(): List<SelectionElement> {
        val viewPeriodOptions = mutableListOf<SelectionElement>()
        for (number in 1 .. 10) {
            viewPeriodOptions.add(SelectionElement(number, number, R.string.chart_view_period_days_count))
        }
        return viewPeriodOptions
    }

    fun startEdit() {
        graphViewPeriodPreviousValue = interactor.getGraphViewPeriod()
    }

    fun stopEdit() {
        if (graphViewPeriodPreviousValue < interactor.getGraphViewPeriod()) {
            interactor.clearLastSync()
        }
    }
}