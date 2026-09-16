package com.ruuvi.station.history

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.network.data.request.GetSensorDataRequest
import com.ruuvi.station.network.data.response.*
import com.ruuvi.station.network.domain.NetworkHistoryInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tagdetails.ui.SensorCardViewModel
import com.ruuvi.station.tagdetails.ui.SensorCardViewModelArguments
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.*
import java.util.concurrent.CopyOnWriteArrayList

class HistoryRetryTest {
    @Test fun `retry from the history screen resumes after completed pages`() = runBlocking<Unit> {
        val now = 1_800_000_000_000L
        val range = HistoryRange.retained(now)
        val network = mock<RuuviNetworkInteractor>()
        val settings = mock<SensorSettingsRepository>()
        val history = mock<SensorHistoryRepository>()
        val preferences = mock<PreferencesRepository>()
        val covered = CopyOnWriteArrayList<HistoryRange>()
        val requests = CopyOnWriteArrayList<GetSensorDataRequest>()
        whenever(network.signedIn).thenReturn(true)
        whenever(network.getEmail()).thenReturn("account")
        whenever(settings.getSensorSettings("A")).thenReturn(SensorSettings(id = "A", networkSensor = true, cloudHistoryDays = 100))
        whenever(history.coverage(any(), any(), any(), any())).thenAnswer { covered.toList() }
        whenever(history.saveCloudPage(any(), any(), any(), any(), any(), any())).thenAnswer {
            covered += it.getArgument<HistoryRange>(2)
            true
        }
        val completed = CompletableDeferred<Unit>()
        whenever(network.getSensorData(any())).doSuspendableAnswer {
            requests += it.getArgument<GetSensorDataRequest>(0)
            when (requests.size) {
                1 -> response(range.startMillis / 1000)
                2 -> null
                else -> { completed.complete(Unit); response() }
            }
        }
        val cloud = NetworkHistoryInteractor(network, settings, history, preferences) { now }
        val viewModel = SensorCardViewModel(SensorCardViewModelArguments(), mock(), mock(), mock(), mock(),
            preferences, mock(), history, mock(), mock(), mock(), mock(), mock(), cloud, { now })
        val job = launch { viewModel.observeHistory("A") }
        try {
            withTimeout(5000) { cloud.state.first { it.failed } }
            viewModel.retryCloudHistory()
            withTimeout(5000) { completed.await() }
            assertEquals(3, requests.size)
            assertEquals(range.startMillis + 1000, requests.last().since!!.time)
        } finally { job.cancelAndJoin() }
    }

    private fun response(vararg seconds: Long): GetSensorDataResponse = RuuviNetworkResponse(
        "success", "", GetSensorDataResponseBody("A", 0.0, 0.0, 0.0, seconds.size,
            seconds.map { SensorDataMeasurementResponse("", "", "", it, -60) }), null)
}
