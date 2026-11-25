package com.ruuvi.station.dashboard.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.tag.domain.RuuviTag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Renders the real dashboard grid, so its item keys are exercised in and out of a drag. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w400dp-h2000dp-mdpi")
class DashboardItemsDragKeyComposeTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val items = mutableStateListOf(sensor("AA:00", "Sensor A"), sensor("BB:00", "Sensor B"))
    private lateinit var state: ItemStaggeredGridDragAndDropState
    private lateinit var gridState: LazyStaggeredGridState

    private fun setContent() {
        compose.setContent {
            gridState = rememberLazyStaggeredGridState()
            state = rememberDragDropStaggeredGridState(
                lazyListState = gridState,
                onMove = { from, to ->
                    val tmp = items[from]
                    items[from] = items[to]
                    items[to] = tmp
                },
                onDoneDragging = {},
            )
            RuuviTheme {
                Box(modifier = Modifier.size(width = 400.dp, height = 2000.dp)) {
                    DashboardItems(
                        items = items,
                        staleSensorIds = emptySet(),
                        userEmail = null,
                        dashboardType = DashboardType.SIMPLE_VIEW,
                        dashboardTapAction = DashboardTapAction.OPEN_CARD,
                        syncCloud = {},
                        setName = { _, _ -> },
                        onMove = { _, _, _ -> },
                        getDefaultImageResource = { R.drawable.new_bg2 },
                        dragDropListState = state,
                        refreshing = false,
                        onShowStaleSnackbar = {},
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun top(text: String) = compose.onNodeWithText(text).getUnclippedBoundsInRoot().top

    @Test
    fun `cards keep showing the right sensor while keys switch to slots during a drag`() {
        setContent()
        assertTrue(top("Sensor A") < top("Sensor B"))

        compose.runOnIdle {
            val first = gridState.layoutInfo.visibleItemsInfo.first()
            state.onDragStart(Offset(first.offset.x + 10f, first.offset.y + 10f))
        }
        compose.waitForIdle()
        assertTrue(state.isDragInProgress)
        assertTrue(top("Sensor A") < top("Sensor B"))

        // A swap while slot-keyed only changes which sensor each slot shows.
        compose.runOnIdle {
            val tmp = items[0]
            items[0] = items[1]
            items[1] = tmp
        }
        compose.waitForIdle()
        assertTrue(top("Sensor B") < top("Sensor A"))

        compose.runOnIdle { state.onDragInterrupted(canceled = true) }
        compose.waitForIdle()
        assertFalse(state.isDragInProgress)
        assertTrue(top("Sensor B") < top("Sensor A"))
    }

    private fun sensor(id: String, displayName: String) = RuuviTag(
        id = id,
        name = displayName,
        displayName = displayName,
        temperatureOffset = null,
        humidityOffset = null,
        pressureOffset = null,
        defaultBackground = R.drawable.new_bg2,
        userBackground = null,
        networkBackground = null,
        alarmSensorStatus = AlarmSensorStatus.NoAlarms,
        lastSync = null,
        networkLastSync = null,
        networkSensor = false,
        owner = null,
        subscriptionName = null,
        firmware = null,
        defaultDisplayOrder = true,
        displayOrder = emptyList(),
        possibleDisplayOptions = emptyList(),
        description = null,
        valuesToDisplay = emptyList(),
        latestMeasurement = null,
    )
}
