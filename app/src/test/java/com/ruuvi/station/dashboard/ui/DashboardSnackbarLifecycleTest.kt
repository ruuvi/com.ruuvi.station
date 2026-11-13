@file:Suppress("UsingMaterialAndMaterial3Libraries")

package com.ruuvi.station.dashboard.ui

import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardSnackbarLifecycleTest {
    @Test
    fun `repeated stale requests reuse the active snackbar job`() = runTest {
        val hostState = SnackbarHostState()
        val firstJob = showSnackbarIfIdle(this, hostState, "Measurements may be outdated", null)
        runCurrent()

        val repeatedJob = showSnackbarIfIdle(
            scope = this,
            hostState = hostState,
            message = "Measurements may be outdated",
            activeJob = firstJob,
        )
        assertSame(firstJob, repeatedJob)

        hostState.currentSnackbarData?.dismiss()
        advanceUntilIdle()

        val nextJob = showSnackbarIfIdle(this, hostState, "Measurements may be outdated", firstJob)
        assertNotSame(firstJob, nextJob)
        nextJob?.cancel()
    }

    @Test
    fun `leaving dashboard cancels and dismisses stale snackbar`() = runTest {
        val hostState = SnackbarHostState()
        val snackbarJob = showSnackbarIfIdle(this, hostState, "Measurements may be outdated", null)
        runCurrent()

        dismissDashboardSnackbar(snackbarJob)
        runCurrent()

        assertTrue(snackbarJob?.isCancelled == true)
        assertNull(hostState.currentSnackbarData)
    }

    @Test
    fun `leaving dashboard without a stale job preserves permission snackbar and settings action`() = runTest {
        val hostState = SnackbarHostState()
        var result: SnackbarResult? = null
        launch {
            result = hostState.showSnackbar("Permission required", actionLabel = "Settings")
        }
        runCurrent()
        val permissionSnackbar = hostState.currentSnackbarData

        dismissDashboardSnackbar(null)
        runCurrent()

        assertSame(permissionSnackbar, hostState.currentSnackbarData)
        assertEquals("Settings", hostState.currentSnackbarData?.actionLabel)
        hostState.currentSnackbarData?.performAction()
        runCurrent()
        assertEquals(SnackbarResult.ActionPerformed, result)
    }

    @Test
    fun `leaving dashboard after stale warning completes preserves permission snackbar`() = runTest {
        val hostState = SnackbarHostState()
        val staleJob = showSnackbarIfIdle(this, hostState, "Measurements may be outdated", null)
        runCurrent()
        hostState.currentSnackbarData?.dismiss()
        runCurrent()
        assertTrue(staleJob?.isCompleted == true)

        var result: SnackbarResult? = null
        launch {
            result = hostState.showSnackbar("Permission required", actionLabel = "Settings")
        }
        runCurrent()
        val permissionSnackbar = hostState.currentSnackbarData

        dismissDashboardSnackbar(staleJob)
        runCurrent()

        assertSame(permissionSnackbar, hostState.currentSnackbarData)
        hostState.currentSnackbarData?.performAction()
        runCurrent()
        assertEquals(SnackbarResult.ActionPerformed, result)
    }

    @Test
    fun `stale request is skipped while another snackbar is visible`() = runTest {
        val hostState = SnackbarHostState()
        val existingJob = launch {
            hostState.showSnackbar("Another message")
        }
        runCurrent()

        val staleJob = showSnackbarIfIdle(this, hostState, "Measurements may be outdated", null)

        assertNull(staleJob)
        hostState.currentSnackbarData?.dismiss()
        existingJob.cancel()
    }
}
