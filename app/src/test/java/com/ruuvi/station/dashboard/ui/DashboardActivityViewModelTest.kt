package com.ruuvi.station.dashboard.ui

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.tag.domain.sensorMeasurementsPreview
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class DashboardActivityViewModelTest {
    private val tagInteractor = mockk<TagInteractor>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private var sensors = listOf(sensor())
    private var timeoutSeconds = 60L

    private val viewModel by lazy {
        DashboardActivityViewModel(
            tagInteractor = tagInteractor,
            networkDataSyncInteractor = mockk(relaxed = true),
            networkInteractor = mockk(relaxed = true),
            preferencesRepository = preferencesRepository,
            permissionLogicInteractor = mockk(relaxed = true),
            networkApplicationSettings = mockk(relaxed = true),
            networkSignInInteractor = mockk(relaxed = true),
            nfcResultInteractor = mockk(relaxed = true),
            sortingInteractor = mockk(relaxed = true),
            runtimeBehavior = mockk(relaxed = true),
            bluetoothInteractor = mockk(relaxed = true),
            marketingConsentInteractor = mockk(relaxed = true),
            imageInteractor = mockk(relaxed = true)
        )
    }

    @Before
    fun setUp() {
        every { tagInteractor.getTags() } answers { sensors }
        every { preferencesRepository.getDashboardCardTimeoutSeconds() } answers { timeoutSeconds }
        every { preferencesRepository.getDashboardType() } returns DashboardType.IMAGE_VIEW
        every { preferencesRepository.getDashboardTapAction() } returns DashboardTapAction.getByCode("")
    }

    @Test
    fun `card becomes stale at the timeout without receiving another update`() {
        viewModel.refreshSensorFreshness(UPDATED_AT + 59_999)
        assertTrue(viewModel.staleSensorIds.value.isEmpty())

        viewModel.refreshSensorFreshness(UPDATED_AT + 60_000)
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
        verify(exactly = 1) { tagInteractor.getTags() }
    }

    @Test
    fun `one second timeout does not mark a card stale early`() {
        timeoutSeconds = 1L

        viewModel.refreshSensorFreshness(UPDATED_AT + 999)
        assertTrue(viewModel.staleSensorIds.value.isEmpty())

        viewModel.refreshSensorFreshness(UPDATED_AT + 1_000)
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
    }

    @Test
    fun `freshness is evaluated independently for local cloud and missing measurements`() {
        sensors = listOf(
            sensor(id = "local-stale", updatedAt = UPDATED_AT - 60_000),
            sensor(id = "local-fresh"),
            sensor(id = "cloud-stale", updatedAt = UPDATED_AT - 60_000).copy(
                networkSensor = true,
                networkLastSync = Date(UPDATED_AT - 60_000)
            ),
            sensor(id = "cloud-fresh").copy(
                networkSensor = true,
                networkLastSync = Date(UPDATED_AT)
            ),
            sensor(id = "no-data").copy(latestMeasurement = null)
        )

        viewModel.refreshSensorFreshness(UPDATED_AT)

        assertEquals(setOf("local-stale", "cloud-stale", "no-data"), viewModel.staleSensorIds.value)
    }

    @Test
    fun `old measurements are stale as soon as the dashboard opens`() {
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
    }

    @Test
    fun `future measurement timestamp is not stale`() {
        viewModel.refreshSensorFreshness(UPDATED_AT - 1_000)

        assertTrue(viewModel.staleSensorIds.value.isEmpty())
    }

    @Test
    fun `updated developer timeout takes effect without recreating the dashboard`() {
        viewModel.refreshSensorFreshness(UPDATED_AT + 30_000)
        assertTrue(viewModel.staleSensorIds.value.isEmpty())

        timeoutSeconds = 5L
        viewModel.refreshSensorFreshness(UPDATED_AT + 30_000)
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)

        timeoutSeconds = 300L
        viewModel.refreshSensorFreshness(UPDATED_AT + 30_000)
        assertTrue(viewModel.staleSensorIds.value.isEmpty())
    }

    @Test
    fun `large timeout does not overflow into a stale card`() {
        timeoutSeconds = Long.MAX_VALUE

        viewModel.refreshSensorFreshness(UPDATED_AT + 60_000)

        assertTrue(viewModel.staleSensorIds.value.isEmpty())
    }

    @Test
    fun `new measurement restores freshness even when sensor values are unchanged`() = runBlocking {
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
        sensors = listOf(sensor(updatedAt = System.currentTimeMillis()))

        viewModel.refreshSensors()

        assertTrue(viewModel.staleSensorIds.value.isEmpty())
    }

    @Test
    fun `card still expires while sensor list updates are paused for scrolling`() {
        viewModel.refreshSensorFreshness(UPDATED_AT)
        viewModel.setSensorListScrolling(true)

        viewModel.refreshSensorFreshness(UPDATED_AT + 60_000)

        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
    }

    @Test
    fun `resuming scrolling restores freshness from a pending measurement`() = runBlocking {
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
        viewModel.setSensorListScrolling(true)
        sensors = listOf(sensor(updatedAt = System.currentTimeMillis()))

        viewModel.refreshSensors()
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
        viewModel.setSensorListScrolling(false)

        assertTrue(viewModel.staleSensorIds.value.isEmpty())
    }

    @Test
    fun `removed sensors no longer appear in stale sensor state`() = runBlocking {
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
        sensors = emptyList()

        viewModel.refreshSensors()

        assertTrue(viewModel.staleSensorIds.value.isEmpty())
    }

    private fun sensor(id: String = "sensor", updatedAt: Long = UPDATED_AT) =
        ruuviTagPreview.copy(
            id = id,
            networkSensor = false,
            networkLastSync = null,
            latestMeasurement = sensorMeasurementsPreview.copy(updatedAt = Date(updatedAt))
        )

    companion object {
        private const val UPDATED_AT = 1_000_000L
    }
}
