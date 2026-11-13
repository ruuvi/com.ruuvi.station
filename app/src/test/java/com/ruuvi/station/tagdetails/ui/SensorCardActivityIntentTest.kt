package com.ruuvi.station.tagdetails.ui

import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.core.content.IntentCompat
import com.ruuvi.station.dashboard.ui.DashboardActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class SensorCardActivityIntentTest {
    @Test
    fun `start launches requested sensor destination without new sensor flag`() {
        withHost { host ->
            SensorCardActivity.start(host, OTHER_SENSOR_ID, SensorCardOpenType.ALERTS)

            assertDetailIntent(
                intent = shadowOf(host).nextStartedActivity,
                sensorId = OTHER_SENSOR_ID,
                openType = SensorCardOpenType.ALERTS,
                newSensor = false,
            )
        }
    }

    @Test
    fun `remove launch requires sensor id and opens removal destination`() {
        withHost { host ->
            SensorCardActivity.startToRemove(host, SENSOR_ID)
            assertDetailIntent(
                intent = shadowOf(host).nextStartedActivity,
                sensorId = SENSOR_ID,
                openType = SensorCardOpenType.REMOVE,
                newSensor = false,
            )
            assertThrows(IllegalArgumentException::class.java) {
                SensorCardActivity.startToRemove(host, null)
            }
        }
    }

    @Test
    fun `dashboard launch creates dashboard then detail back stack`() {
        withHost { host ->
            SensorCardActivity.startWithDashboard(
                context = host,
                sensorId = SENSOR_ID,
                openType = SensorCardOpenType.HISTORY,
            )

            val shadow = shadowOf(host)
            val detailIntent = shadow.nextStartedActivity
            val dashboardIntent = shadow.nextStartedActivity
            assertEquals(DashboardActivity::class.java.name, dashboardIntent.component?.className)
            assertDetailIntent(
                intent = detailIntent,
                sensorId = SENSOR_ID,
                openType = SensorCardOpenType.HISTORY,
                newSensor = false,
            )
        }
    }

    @Test
    fun `new sensor launch creates settings detail with new sensor flag`() {
        withHost { host ->
            SensorCardActivity.startAfterAddingNewSensor(host, SENSOR_ID)

            val shadow = shadowOf(host)
            val detailIntent = shadow.nextStartedActivity
            val dashboardIntent = shadow.nextStartedActivity
            assertEquals(DashboardActivity::class.java.name, dashboardIntent.component?.className)
            assertDetailIntent(
                intent = detailIntent,
                sensorId = SENSOR_ID,
                openType = SensorCardOpenType.SETTINGS,
                newSensor = true,
            )
        }
    }

    @Test
    fun `standard pending intent retains stack request code flags and destination`() {
        withHost { host ->
            val pendingIntent = requireNotNull(
                SensorCardActivity.createPendingIntent(
                    context = host,
                    sensorId = SENSOR_ID,
                    requestCode = REQUEST_CODE,
                    openType = SensorCardOpenType.CARD,
                ),
            )
            val shadow = shadowOf(pendingIntent)

            assertEquals(REQUEST_CODE, shadow.requestCode)
            assertTrue((shadow.flags and PendingIntent.FLAG_UPDATE_CURRENT) != 0)
            assertTrue((shadow.flags and PendingIntent.FLAG_IMMUTABLE) != 0)
            assertEquals(2, shadow.savedIntents.size)
            assertEquals(DashboardActivity::class.java.name, shadow.savedIntents[0].component?.className)
            assertDetailIntent(
                intent = shadow.savedIntents[1],
                sensorId = SENSOR_ID,
                openType = SensorCardOpenType.CARD,
                newSensor = false,
            )
        }
    }

    @Test
    fun `widget pending intent has stable widget and sensor identity`() {
        withHost { host ->
            val first = requireNotNull(
                SensorCardActivity.createWidgetPendingIntent(host, SENSOR_ID, APP_WIDGET_ID),
            )
            val otherSensor = requireNotNull(
                SensorCardActivity.createWidgetPendingIntent(host, OTHER_SENSOR_ID, APP_WIDGET_ID),
            )

            val firstDetail = shadowOf(first).savedIntents.last()
            val otherDetail = shadowOf(otherSensor).savedIntents.last()
            assertEquals("ruuvi-station", firstDetail.data?.scheme)
            assertEquals("widget", firstDetail.data?.authority)
            assertEquals(
                listOf(APP_WIDGET_ID.toString(), SENSOR_ID),
                firstDetail.data?.pathSegments,
            )
            assertEquals(
                listOf(APP_WIDGET_ID.toString(), OTHER_SENSOR_ID),
                otherDetail.data?.pathSegments,
            )
            assertNotEquals(firstDetail.data, otherDetail.data)
            assertFalse(firstDetail.getBooleanExtra(SensorCardActivity.ARGUMENT_NEW_SENSOR, true))
        }
    }

    private fun assertDetailIntent(
        intent: Intent,
        sensorId: String,
        openType: SensorCardOpenType,
        newSensor: Boolean,
    ) {
        assertEquals(SensorCardActivity::class.java.name, intent.component?.className)
        assertEquals(sensorId, intent.getStringExtra(SensorCardActivity.ARGUMENT_SENSOR_ID))
        assertEquals(
            openType,
            IntentCompat.getSerializableExtra(
                intent,
                SensorCardActivity.ARGUMENT_OPEN_TYPE,
                SensorCardOpenType::class.java,
            ),
        )
        assertEquals(
            newSensor,
            intent.getBooleanExtra(SensorCardActivity.ARGUMENT_NEW_SENSOR, false),
        )
    }

    private fun withHost(block: (Activity) -> Unit) {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            block(controller.get())
        } finally {
            controller.pause().stop().destroy()
        }
    }

    private companion object {
        const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
        const val OTHER_SENSOR_ID = "11:22:33:44:55:66"
        const val REQUEST_CODE = 42
        const val APP_WIDGET_ID = 17
    }
}
