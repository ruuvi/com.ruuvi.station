package com.ruuvi.station.history

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.*
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.graph.model.ChartContainer
import com.ruuvi.station.network.domain.NetworkHistoryInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import com.ruuvi.station.tagdetails.ui.SensorCardViewModel
import com.ruuvi.station.tagdetails.ui.SensorCardViewModelArguments
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.UnitType
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

class HistoryGraphPipelineTest {
    @Test fun `graphs cache unchanged data and zoom reads local detail without cloud work`() = runBlocking<Unit> {
        val repository = mock<SensorHistoryRepository>()
        val tags = mock<TagRepository>()
        val settings = mock<SensorSettingsRepository>()
        val preferences = mock<PreferencesRepository>()
        val units = mock<UnitsConverter>()
        val cloud = mock<NetworkHistoryInteractor>()
        val sensor = mock<RuuviTag>()
        val unit = UnitType.TemperatureUnit.Celsius
        whenever(sensor.displayOrder).thenReturn(listOf(unit))
        whenever(tags.getFavoriteSensorById("A")).thenReturn(sensor)
        whenever(settings.getSensorSettings("A")).thenReturn(SensorSettings(id = "A"))
        whenever(units.getTemperatureValue(any<Double>(), eq(unit))).thenAnswer { it.getArgument<Double>(0) }
        val queryCount = AtomicInteger()
        val current = System.currentTimeMillis()
        val start = current - 99 * 86_400_000L
        doAnswer { invocation ->
            queryCount.incrementAndGet()
            val range = invocation.getArgument<HistoryRange>(1)
            val consume = invocation.getArgument<(TagSensorReading) -> Unit>(2)
            repeat(142560) { i ->
                val time = start + i * 60_000L
                if (time in range.startMillis until range.endExclusiveMillis)
                    consume(TagSensorReading(ruuviTagId = "A", createdAt = Date(time), temperature = (i % 17).toDouble()))
            }
            null
        }.whenever(repository).forEachReading(eq("A"), any(), any())
        val interactor = TagDetailsInteractor(tags, repository, settings, units, preferences)
        val viewModel = SensorCardViewModel(SensorCardViewModelArguments(), mock(), interactor, mock(), mock(),
            preferences, mock(), repository, mock(), mock(), mock(), mock(), units, cloud, { current })
        clearInvocations(cloud)
        val outputs = Channel<MutableList<ChartContainer>>(Channel.UNLIMITED)
        val job = launch { viewModel.historyUpdater("A").collect { outputs.send(it) } }
        try {
            val first = withTimeout(5000) { outputs.receive() }.single()
            assertTrue(first.data!!.size <= HistorySampler.MAX_POINTS)
            assertEquals(142560L, first.statistics!!.count)
            delay(1100)
            assertEquals(1, queryCount.get())
            val zoom = HistoryRange(start + 20 * 86_400_000L, start + 20 * 86_400_000L + 600_000)
            viewModel.setHistoryViewport("A", zoom, true)
            val detail = withTimeout(5000) { outputs.receive() }.single()
            assertEquals(10, detail.data!!.size)
            assertTrue(detail.data!!.all { (it.data as HistoryPoint).timestamp in zoom.startMillis until zoom.endExclusiveMillis })
            val overviewTimes = first.data!!.map { (it.data as HistoryPoint).timestamp }.toSet()
            assertTrue(detail.data!!.any { (it.data as HistoryPoint).timestamp !in overviewTimes })
            val mini = viewModel.getChartData("A", unit, 2400).first()
            assertTrue(mini.segments.sumOf { it.timestamps.size } <= HistorySampler.MAX_POINTS)
            assertEquals(0.0, mini.minValue, 0.0)
            assertEquals(16.0, mini.maxValue, 0.0)
            verifyNoInteractions(cloud)
        } finally { job.cancelAndJoin() }
    }
}
