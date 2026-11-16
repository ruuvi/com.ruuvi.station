@file:Suppress("UsingMaterialAndMaterial3Libraries")

package com.ruuvi.station.dashboard.ui

import android.app.Application
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class DashboardSnackbarLifecycleEffectTest {
    @get:Rule
    val compose = createComposeRule()

    private val owner = TestLifecycleOwner()
    private val hostState = SnackbarHostState()
    private lateinit var scope: CoroutineScope
    private var staleJob by mutableStateOf<Job?>(null)
    private var stopCount = 0

    @Test
    fun `observer keeps warning on pause and cancels latest stale job on stop`() {
        setContent()
        compose.runOnIdle {
            staleJob = showSnackbarIfIdle(scope, hostState, "Measurements may be outdated", null)
        }
        compose.waitForIdle()
        val warningJob = staleJob
        compose.runOnIdle {
            assertEquals("Measurements may be outdated", hostState.currentSnackbarData?.message)
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        compose.runOnIdle {
            assertTrue(warningJob?.isActive == true)
            assertEquals(0, stopCount)
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        compose.runOnIdle {
            assertTrue(warningJob?.isCancelled == true)
            assertNull(staleJob)
            assertNull(hostState.currentSnackbarData)
            assertEquals(1, stopCount)
        }
    }

    @Test
    fun `observer preserves permission snackbar and settings action without a stale job`() {
        setContent()
        assertPermissionSurvivesStop()
    }

    @Test
    fun `observer preserves permission snackbar after stale warning completes`() {
        setContent()
        compose.runOnIdle {
            staleJob = showSnackbarIfIdle(scope, hostState, "Measurements may be outdated", null)
        }
        compose.runOnIdle {
            hostState.currentSnackbarData?.dismiss()
        }
        compose.runOnIdle {
            assertTrue(staleJob?.isCompleted == true)
        }

        assertPermissionSurvivesStop()
    }

    private fun setContent() {
        compose.setContent {
            scope = rememberCoroutineScope()
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                DashboardSnackbarLifecycleEffect(staleJob) {
                    staleJob = null
                    stopCount++
                }
            }
        }
        compose.waitForIdle()
    }

    private fun assertPermissionSurvivesStop() {
        var result: SnackbarResult? = null
        lateinit var permissionJob: Job
        compose.runOnIdle {
            permissionJob = scope.launch {
                result = hostState.showSnackbar("Permission required", actionLabel = "Settings")
            }
        }
        compose.runOnIdle {
            val permissionSnackbar = hostState.currentSnackbarData
            assertEquals("Permission required", permissionSnackbar?.message)
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            assertSame(permissionSnackbar, hostState.currentSnackbarData)
        }
        compose.runOnIdle {
            assertNull(staleJob)
            assertEquals(1, stopCount)
            assertFalse(permissionJob.isCancelled)
            assertEquals("Settings", hostState.currentSnackbarData?.actionLabel)
            hostState.currentSnackbarData?.performAction()
        }
        compose.runOnIdle {
            assertEquals(SnackbarResult.ActionPerformed, result)
        }
    }

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).apply {
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        override val lifecycle: Lifecycle = registry
    }
}
