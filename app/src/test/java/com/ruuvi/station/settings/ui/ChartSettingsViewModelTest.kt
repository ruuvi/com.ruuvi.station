package com.ruuvi.station.settings.ui

import com.ruuvi.station.settings.domain.AppSettingsInteractor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChartSettingsViewModelTest {

    private lateinit var interactor: AppSettingsInteractor
    private lateinit var viewModel: ChartSettingsViewModel

    @Before
    fun setUp() {
        interactor = mockk(relaxed = true)
        every { interactor.getGraphViewPeriod() } returns 2
        every { interactor.isShowAllGraphPoint() } returns false
        every { interactor.graphDrawDots() } returns true

        viewModel = ChartSettingsViewModel(interactor)
    }

    @Test
    fun `setViewPeriod updates interactor and state flow`() {
        every { interactor.getGraphViewPeriod() } returns 5
        viewModel.setViewPeriod(5)

        verify { interactor.setGraphViewPeriod(5) }
        assertEquals(5, viewModel.viewPeriod.value)
    }

    @Test
    fun `setShowAllPoints updates interactor and state flow`() {
        every { interactor.isShowAllGraphPoint() } returns true
        viewModel.setShowAllPoints(true)

        verify { interactor.setIsShowAllGraphPoint(true) }
        assertTrue(viewModel.showAllPoints.value)
    }

    @Test
    fun `setDrawDots updates interactor and state flow`() {
        every { interactor.graphDrawDots() } returns true
        viewModel.setDrawDots(true)

        verify { interactor.setGraphDrawDots(true) }
        assertTrue(viewModel.drawDots.value)
    }

    @Test
    fun `getViewPeriodOptions returns 10 options`() {
        val options = viewModel.getViewPeriodOptions()
        assertEquals(10, options.size)
        assertEquals(1, options.first().value)
        assertEquals(10, options.last().value)
    }

    @Test
    fun `stopEdit clears sync if period increased`() {
        every { interactor.getGraphViewPeriod() } returns 2
        viewModel.startEdit()

        every { interactor.getGraphViewPeriod() } returns 7
        viewModel.stopEdit()

        verify { interactor.clearLastSync() }
    }
}
