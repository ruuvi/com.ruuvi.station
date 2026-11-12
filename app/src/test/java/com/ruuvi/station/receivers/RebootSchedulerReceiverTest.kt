package com.ruuvi.station.receivers

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.bluetooth.BluetoothForegroundService
import com.ruuvi.station.util.BackgroundScanModes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RebootSchedulerReceiverTest {
    private val context: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        Preferences(context).backgroundScanMode = BackgroundScanModes.BACKGROUND
        // The receiver ignores broadcasts while the app is in the foreground.
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        shadowOf(activityManager).setProcesses(
            listOf(
                ActivityManager.RunningAppProcessInfo().apply {
                    pid = Process.myPid()
                    importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED
                }
            )
        )
    }

    @Test
    fun `manifest intent filter matches the expected actions`() {
        assertEquals(RebootSchedulerReceiver.EXPECTED_ACTIONS, manifestActions())
    }

    @Test
    fun `every manifest action starts background scanning`() {
        manifestActions().forEach { action ->
            RebootSchedulerReceiver().onReceive(context, Intent(action))

            assertScanServiceStarted(action)
        }
    }

    @Test
    fun `boot completed starts background scanning`() {
        RebootSchedulerReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertScanServiceStarted(Intent.ACTION_BOOT_COMPLETED)
    }

    @Test
    fun `quickboot actions are ignored`() {
        // Not protected broadcasts, so any app could send them.
        listOf(
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        ).forEach { action ->
            RebootSchedulerReceiver().onReceive(context, Intent(action))

            assertNull(action, shadowOf(context).nextStartedService)
        }
    }

    @Test
    fun `unexpected action is ignored`() {
        RebootSchedulerReceiver().onReceive(context, Intent("com.example.SPOOFED_ACTION"))

        assertNull(shadowOf(context).nextStartedService)
    }

    @Test
    fun `intent without action is ignored`() {
        RebootSchedulerReceiver().onReceive(context, Intent())

        assertNull(shadowOf(context).nextStartedService)
    }

    private fun manifestActions(): Set<String> =
        shadowOf(context.packageManager)
            .getIntentFiltersForReceiver(ComponentName(context, RebootSchedulerReceiver::class.java))
            .flatMap { filter -> (0 until filter.countActions()).map(filter::getAction) }
            .toSet()

    private fun assertScanServiceStarted(action: String) {
        val started = shadowOf(context).nextStartedService
        assertNotNull(action, started)
        assertEquals(action, BluetoothForegroundService::class.java.name, started.component?.className)
    }
}
