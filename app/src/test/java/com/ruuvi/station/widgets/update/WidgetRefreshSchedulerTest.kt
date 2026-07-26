package com.ruuvi.station.widgets.update

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetRefreshSchedulerTest {

    @Test
    fun `repeated simple refreshes use one coalescing work name`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        val target = WidgetRefreshTarget(WidgetRefreshType.SIMPLE)

        repeat(2) {
            WidgetRefreshScheduler.enqueue(workManager, target)
        }

        verify(exactly = 2) {
            workManager.enqueueUniqueWork(
                target.uniqueWorkName,
                ExistingWorkPolicy.KEEP,
                any<OneTimeWorkRequest>(),
            )
        }
    }

    @Test
    fun `simple and complex refreshes use separate unique work`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        val simpleTarget = WidgetRefreshTarget(WidgetRefreshType.SIMPLE)
        val complexTarget = WidgetRefreshTarget(WidgetRefreshType.COMPLEX)

        WidgetRefreshScheduler.enqueue(workManager, simpleTarget)
        WidgetRefreshScheduler.enqueue(workManager, complexTarget)

        assertNotEquals(
            WidgetRefreshType.SIMPLE.uniqueWorkName,
            WidgetRefreshType.COMPLEX.uniqueWorkName,
        )
        assertNotEquals(
            WidgetRefreshType.SIMPLE.workTag,
            WidgetRefreshType.COMPLEX.workTag,
        )
        verify {
            workManager.enqueueUniqueWork(
                simpleTarget.uniqueWorkName,
                ExistingWorkPolicy.KEEP,
                any<OneTimeWorkRequest>(),
            )
            workManager.enqueueUniqueWork(
                complexTarget.uniqueWorkName,
                ExistingWorkPolicy.KEEP,
                any<OneTimeWorkRequest>(),
            )
        }
    }

    @Test
    fun `refresh request has common and provider tags`() {
        val target = WidgetRefreshTarget(WidgetRefreshType.COMPLEX)
        val request = WidgetRefreshScheduler.createRequest(target)

        assertTrue(request.tags.contains(WidgetRefreshScheduler.WIDGET_REFRESH_WORK_TAG))
        assertTrue(request.tags.contains(WidgetRefreshType.COMPLEX.workTag))
    }

    @Test
    fun `all and widget-specific targets round trip through worker data`() {
        WidgetRefreshType.entries.forEach { refreshType ->
            val refreshAll = WidgetRefreshTarget(refreshType)
            val refreshOne = WidgetRefreshTarget(refreshType, appWidgetId = 42)

            assertEquals(
                refreshAll,
                WidgetRefreshTarget.fromInputData(refreshAll.toInputData()),
            )
            assertEquals(
                refreshOne,
                WidgetRefreshTarget.fromInputData(refreshOne.toInputData()),
            )
        }
    }

    @Test
    fun `widget-specific work has a provider and widget specific identity`() {
        val simpleWidget = WidgetRefreshTarget(WidgetRefreshType.SIMPLE, appWidgetId = 12)
        val otherSimpleWidget = WidgetRefreshTarget(WidgetRefreshType.SIMPLE, appWidgetId = 13)
        val complexWidget = WidgetRefreshTarget(WidgetRefreshType.COMPLEX, appWidgetId = 12)

        assertNotEquals(simpleWidget.uniqueWorkName, otherSimpleWidget.uniqueWorkName)
        assertNotEquals(simpleWidget.uniqueWorkName, complexWidget.uniqueWorkName)
        assertNotEquals(
            simpleWidget.uniqueWorkName,
            WidgetRefreshTarget(WidgetRefreshType.SIMPLE).uniqueWorkName,
        )
    }

    @Test
    fun `malformed worker data is rejected`() {
        assertNull(WidgetRefreshTarget.fromInputData(Data.EMPTY))
        assertNull(
            WidgetRefreshTarget.fromInputData(
                workDataOf(
                    WidgetRefreshScheduler.WIDGET_REFRESH_TYPE_KEY to "unknown",
                    WidgetRefreshScheduler.WIDGET_REFRESH_SCOPE_KEY to "all",
                ),
            ),
        )
        assertNull(
            WidgetRefreshTarget.fromInputData(
                workDataOf(
                    WidgetRefreshScheduler.WIDGET_REFRESH_TYPE_KEY to "simple",
                    WidgetRefreshScheduler.WIDGET_REFRESH_SCOPE_KEY to "single",
                ),
            ),
        )
        assertNull(
            WidgetRefreshTarget.fromInputData(
                workDataOf(
                    WidgetRefreshScheduler.WIDGET_REFRESH_TYPE_KEY to "simple",
                    WidgetRefreshScheduler.WIDGET_REFRESH_SCOPE_KEY to "single",
                    WidgetRefreshScheduler.APP_WIDGET_ID_KEY to 0,
                ),
            ),
        )
    }

    @Test
    fun `widget-specific target rejects invalid IDs`() {
        assertThrows(IllegalArgumentException::class.java) {
            WidgetRefreshTarget(WidgetRefreshType.SIMPLE, appWidgetId = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WidgetRefreshTarget(WidgetRefreshType.SIMPLE, appWidgetId = -1)
        }
    }
}
