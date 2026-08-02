package com.ruuvi.station.widgets.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetConfigurationTest {

    @Test
    fun `successful initial update completes before success is returned`() = runBlocking {
        var updateCompleted = false
        var failureHandled = false

        val succeeded = runInitialWidgetUpdate(
            update = {
                yield()
                updateCompleted = true
            },
            onFailure = { failureHandled = true },
        )

        assertTrue(updateCompleted)
        assertTrue(succeeded)
        assertFalse(failureHandled)
    }

    @Test
    fun `ordinary initial update failure is reported without success`() = runBlocking {
        val failure = IllegalStateException("Unable to render")
        var handledFailure: Exception? = null

        val succeeded = runInitialWidgetUpdate(
            update = { throw failure },
            onFailure = { handledFailure = it },
        )

        assertFalse(succeeded)
        assertSame(failure, handledFailure)
    }

    @Test
    fun `initial update cancellation is propagated`() {
        var failureHandled = false

        assertThrows(CancellationException::class.java) {
            runBlocking {
                runInitialWidgetUpdate(
                    update = { throw CancellationException("Cancelled") },
                    onFailure = { failureHandled = true },
                )
            }
        }

        assertFalse(failureHandled)
    }
}
