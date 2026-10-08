package com.ruuvi.station.tagdetails.ui

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.domain.BluetoothGattInteractor
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.export.CsvExporter
import com.ruuvi.station.export.XlsxExporter
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.nfc.domain.NfcResultInteractor
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.tag.domain.sensorMeasurementsPreview
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import java.util.Date

class SensorCardViewModelTest {
    private val tagInteractor = mockk<TagInteractor>(relaxed = true)
    private val tagDetailsInteractor = mockk<TagDetailsInteractor>()
    private val networkDataSyncInteractor = mockk<NetworkDataSyncInteractor>()
    private val appSettingsInteractor = mockk<AppSettingsInteractor>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val gattInteractor = mockk<BluetoothGattInteractor>(relaxed = true)
    private val viewModel by lazy {
        SensorCardViewModel(
            arguments = SensorCardViewModelArguments(),
            tagInteractor = tagInteractor,
            tagDetailsInteractor = tagDetailsInteractor,
            networkDataSyncInteractor = networkDataSyncInteractor,
            appSettingsInteractor = appSettingsInteractor,
            preferencesRepository = preferencesRepository,
            gattInteractor = gattInteractor,
            sensorHistoryRepository = mockk<SensorHistoryRepository>(relaxed = true),
            csvExporter = mockk<CsvExporter>(relaxed = true),
            xlsxExporter = mockk<XlsxExporter>(relaxed = true),
            nfcResultInteractor = mockk<NfcResultInteractor>(relaxed = true),
            alarmRepository = mockk<AlarmRepository>(relaxed = true),
            unitsConverter = mockk<UnitsConverter>(relaxed = true),
        )
    }

    @Before
    fun setUp() {
        every { networkDataSyncInteractor.syncInProgressFlow } returns MutableStateFlow(false)
        every { tagDetailsInteractor.getTagById(AIR_SENSOR.id) } returns AIR_SENSOR
    }

    @Test
    fun `selected Air sensor starts GATT history sync`() {
        viewModel.autoSyncGattHistory(AIR_SENSOR, selected = true)

        verify { gattInteractor.readLogs(AIR_SENSOR.id, LAST_SYNC) }
    }

    @Test
    fun `unselected Air sensor does not start GATT history sync`() {
        viewModel.autoSyncGattHistory(AIR_SENSOR, selected = false)

        verify(exactly = 0) { gattInteractor.readLogs(any(), any()) }
    }

    @Test
    fun `selected non-Air sensor does not start GATT history sync`() {
        viewModel.autoSyncGattHistory(ruuviTagPreview, selected = true)

        verify(exactly = 0) { gattInteractor.readLogs(any(), any()) }
    }

    private companion object {
        val LAST_SYNC = Date()
        val AIR_SENSOR = ruuviTagPreview.copy(
            id = "AA:BB:CC:DD:EE:FF",
            lastSync = LAST_SYNC,
            latestMeasurement = sensorMeasurementsPreview.copy(dataFormat = 0xE0),
        )
    }
}
