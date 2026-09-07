package com.ruuvi.station.widgets.update

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.util.concurrent.TimeUnit

class WidgetRefreshSchedulerTest {

    @Test
    fun `repeated simple refreshes use one coalescing work name`() {
        val workManager = mock<WorkManager>()
        val target = WidgetRefreshTarget(WidgetRefreshType.SIMPLE)

        repeat(2) {
            WidgetRefreshScheduler.enqueue(workManager, target)
        }

        verify(workManager, times(2)).enqueueUniqueWork(
            eq(target.uniqueWorkName),
            eq(ExistingWorkPolicy.KEEP),
            any<OneTimeWorkRequest>(),
        )
    }

    @Test
    fun `simple and complex refreshes use separate unique work`() {
        val workManager = mock<WorkManager>()
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
        verify(workManager).enqueueUniqueWork(
            eq(simpleTarget.uniqueWorkName),
            eq(ExistingWorkPolicy.KEEP),
            any<OneTimeWorkRequest>(),
        )
        verify(workManager).enqueueUniqueWork(
            eq(complexTarget.uniqueWorkName),
            eq(ExistingWorkPolicy.KEEP),
            any<OneTimeWorkRequest>(),
        )
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
            WidgetRefreshTrigger.entries.forEach { refreshTrigger ->
                val refreshAll = WidgetRefreshTarget(refreshType, refreshTrigger = refreshTrigger)
                val refreshOne = WidgetRefreshTarget(
                    refreshType = refreshType,
                    appWidgetId = 42,
                    refreshTrigger = refreshTrigger,
                )

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
        assertNull(
            WidgetRefreshTarget.fromInputData(
                workDataOf(
                    WidgetRefreshScheduler.WIDGET_REFRESH_TYPE_KEY to "simple",
                    WidgetRefreshScheduler.WIDGET_REFRESH_SCOPE_KEY to "all",
                    WidgetRefreshScheduler.WIDGET_REFRESH_TRIGGER_KEY to "invalid",
                ),
            ),
        )
    }

    @Test
    fun `missing worker trigger defaults to automatic`() {
        val target = WidgetRefreshTarget.fromInputData(
            workDataOf(
                WidgetRefreshScheduler.WIDGET_REFRESH_TYPE_KEY to "simple",
                WidgetRefreshScheduler.WIDGET_REFRESH_SCOPE_KEY to "all",
            ),
        )

        assertEquals(
            WidgetRefreshTarget(
                refreshType = WidgetRefreshType.SIMPLE,
                refreshTrigger = WidgetRefreshTrigger.AUTOMATIC,
            ),
            target,
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

    @Test
    fun `automatic widget-specific refresh request is delayed for coalescing`() {
        val target = WidgetRefreshTarget(
            refreshType = WidgetRefreshType.SIMPLE,
            appWidgetId = 42,
            refreshTrigger = WidgetRefreshTrigger.AUTOMATIC,
        )
        val request = WidgetRefreshScheduler.createRequest(target)

        assertEquals(
            TimeUnit.SECONDS.toMillis(2),
            request.workSpec.initialDelay,
        )
    }

    @Test
    fun `manual widget-specific refresh request does not use coalescing delay`() {
        val target = WidgetRefreshTarget(
            refreshType = WidgetRefreshType.SIMPLE,
            appWidgetId = 42,
            refreshTrigger = WidgetRefreshTrigger.MANUAL,
        )
        val request = WidgetRefreshScheduler.createRequest(target)

        assertEquals(0L, request.workSpec.initialDelay)
    }

    @Test
    fun `all-widget automatic refresh request does not use coalescing delay`() {
        val target = WidgetRefreshTarget(
            refreshType = WidgetRefreshType.SIMPLE,
            refreshTrigger = WidgetRefreshTrigger.AUTOMATIC,
        )
        val request = WidgetRefreshScheduler.createRequest(target)

        assertEquals(0L, request.workSpec.initialDelay)
    }

    @Test
    fun `simple widget ids can be filtered by sensor id`() {
        val matched = WidgetRefreshScheduler.matchingSimpleWidgetIdsBySensor(
            appWidgetIds = intArrayOf(1, 2, 3, 4),
            sensorId = "aa:bb:cc:dd",
            sensorIdForWidget = { appWidgetId ->
                when (appWidgetId) {
                    1 -> "AA:BB:CC:DD"
                    2 -> "11:22:33:44"
                    3 -> " aa:bb:cc:dd "
                    else -> null
                }
            },
        )

        assertTrue(matched.contentEquals(intArrayOf(1, 3)))
    }

    @Test
    fun `complex widget ids can be filtered by sensor id`() {
        val matched = WidgetRefreshScheduler.matchingComplexWidgetIdsBySensor(
            appWidgetIds = intArrayOf(10, 11, 12),
            sensorId = "sensor-b",
            sensorIdsForWidget = { appWidgetId ->
                when (appWidgetId) {
                    10 -> listOf("sensor-a")
                    11 -> listOf(" sensor-b ", "sensor-c")
                    12 -> listOf("SENSOR-B")
                    else -> emptyList()
                }
            },
        )

        assertTrue(matched.contentEquals(intArrayOf(11, 12)))
    }
}
