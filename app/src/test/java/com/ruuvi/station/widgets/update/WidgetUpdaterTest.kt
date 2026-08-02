package com.ruuvi.station.widgets.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetUpdaterTest {

    @Test
    fun `widget locks use a bounded stable stripe`() {
        val stripeCount = 32

        assertEquals(
            widgetLockStripeIndex(appWidgetId = 42, stripeCount = stripeCount),
            widgetLockStripeIndex(appWidgetId = 42, stripeCount = stripeCount),
        )
        assertEquals(10, widgetLockStripeIndex(appWidgetId = 42, stripeCount = stripeCount))
        assertEquals(31, widgetLockStripeIndex(appWidgetId = -1, stripeCount = stripeCount))
    }

    @Test
    fun `widgets are updated sequentially in the supplied order`() = runBlocking {
        val updateOrder = mutableListOf<Int>()
        var activeUpdates = 0
        var maximumActiveUpdates = 0

        updateWidgetsSequentially(
            appWidgetIds = intArrayOf(3, 1, 2),
            update = { appWidgetId ->
                activeUpdates += 1
                maximumActiveUpdates = maxOf(maximumActiveUpdates, activeUpdates)
                updateOrder += appWidgetId
                yield()
                activeUpdates -= 1
            },
            onFailure = { _, _ -> error("Unexpected update failure") },
        )

        assertEquals(listOf(3, 1, 2), updateOrder)
        assertEquals(1, maximumActiveUpdates)
    }

    @Test
    fun `one widget failure does not stop later updates`() = runBlocking {
        val attemptedWidgetIds = mutableListOf<Int>()
        val failures = mutableListOf<Pair<Int, Exception>>()

        updateWidgetsSequentially(
            appWidgetIds = intArrayOf(1, 2, 3),
            update = { appWidgetId ->
                attemptedWidgetIds += appWidgetId
                if (appWidgetId == 2) throw IllegalStateException("Failed widget")
            },
            onFailure = { appWidgetId, error -> failures += appWidgetId to error },
        )

        assertEquals(listOf(1, 2, 3), attemptedWidgetIds)
        assertEquals(listOf(2), failures.map { it.first })
        assertTrue(failures.single().second is IllegalStateException)
    }

    @Test
    fun `cancellation stops the batch and is propagated`() {
        val attemptedWidgetIds = mutableListOf<Int>()

        assertThrows(CancellationException::class.java) {
            runBlocking {
                updateWidgetsSequentially(
                    appWidgetIds = intArrayOf(1, 2, 3),
                    update = { appWidgetId ->
                        attemptedWidgetIds += appWidgetId
                        if (appWidgetId == 2) throw CancellationException("Cancelled")
                    },
                    onFailure = { _, _ -> error("Cancellation must not be handled as a failure") },
                )
            }
        }

        assertEquals(listOf(1, 2), attemptedWidgetIds)
    }

    @Test
    fun `empty widget list performs no update`() = runBlocking {
        var updateCalled = false
        var failureCalled = false

        updateWidgetsSequentially(
            appWidgetIds = intArrayOf(),
            update = { updateCalled = true },
            onFailure = { _, _ -> failureCalled = true },
        )

        assertFalse(updateCalled)
        assertFalse(failureCalled)
    }
}
