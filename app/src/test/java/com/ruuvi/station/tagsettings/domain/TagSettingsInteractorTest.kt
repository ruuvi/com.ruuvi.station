package com.ruuvi.station.tagsettings.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.image.ImageInteractor
import com.ruuvi.station.network.data.response.SensorSettings_defaultDisplayOrder
import com.ruuvi.station.network.data.response.SensorSettings_description
import com.ruuvi.station.network.domain.OperationStatus
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TagSettingsInteractorTest {
    private val tagRepository = mockk<TagRepository>(relaxed = true)
    private val sensorSettingsRepository = mockk<SensorSettingsRepository>(relaxed = true)
    private val networkInteractor = mockk<RuuviNetworkInteractor>(relaxed = true)
    private val imageInteractor = mockk<ImageInteractor>(relaxed = true)
    private val interactor = TagSettingsInteractor(
        tagRepository = tagRepository,
        preferencesRepository = mockk<PreferencesRepository>(),
        sensorSettingsRepository = sensorSettingsRepository,
        networkInteractor = networkInteractor,
        imageInteractor = imageInteractor,
    )

    @Test
    fun `deleting an owned network sensor removes it locally then unclaims it`() {
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns SensorSettings(
            id = SENSOR_ID,
            networkSensor = true,
            owner = EMAIL,
        )
        every { networkInteractor.getEmail() } returns EMAIL

        interactor.deleteTagsAndRelatives(SENSOR_ID, deleteData = true)

        verify { tagRepository.deleteSensorAndRelatives(SENSOR_ID) }
        verify { networkInteractor.unclaimSensor(SENSOR_ID, true) }
        verify(exactly = 0) { networkInteractor.unshareSensor(any(), any()) }
    }

    @Test
    fun `deleting a shared network sensor unshares it from the current user`() {
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns SensorSettings(
            id = SENSOR_ID,
            networkSensor = true,
            owner = "owner@example.com",
        )
        every { networkInteractor.getEmail() } returns EMAIL

        interactor.deleteTagsAndRelatives(SENSOR_ID, deleteData = false)

        verify { tagRepository.deleteSensorAndRelatives(SENSOR_ID) }
        verify { networkInteractor.unshareSensor(EMAIL, SENSOR_ID) }
        verify(exactly = 0) { networkInteractor.unclaimSensor(any(), any()) }
    }

    @Test
    fun `local sensor settings changes are not sent to the network`() {
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns SensorSettings(id = SENSOR_ID)

        interactor.setUseDefaultSensorsOrder(SENSOR_ID, useDefault = false)

        verify { sensorSettingsRepository.updateUseDefaultSensorOrder(eq(SENSOR_ID), false, any()) }
        verify(exactly = 0) { networkInteractor.updateSensorSetting(any(), any(), any(), any()) }
    }

    @Test
    fun `network sensor display order change is persisted and queued with one timestamp`() {
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns SensorSettings(
            id = SENSOR_ID,
            networkSensor = true,
        )

        interactor.setUseDefaultSensorsOrder(SENSOR_ID, useDefault = false)

        val localTimestamp = slot<Long>()
        val networkTimestamp = slot<Long>()
        verify {
            sensorSettingsRepository.updateUseDefaultSensorOrder(SENSOR_ID, false, capture(localTimestamp))
            networkInteractor.updateSensorSetting(
                SENSOR_ID,
                SensorSettings_defaultDisplayOrder,
                "false",
                capture(networkTimestamp),
            )
        }
        assertEquals(localTimestamp.captured, networkTimestamp.captured)
    }

    @Test
    fun `network description change is persisted and returns the network operation flow`() {
        val status = emptyFlow<OperationStatus>()
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns SensorSettings(
            id = SENSOR_ID,
            networkSensor = true,
        )
        every {
            networkInteractor.updateSensorSettingWithStatus(SENSOR_ID, SensorSettings_description, "Kitchen", any())
        } returns status

        val result = interactor.updateDescriptionWithStatus(SENSOR_ID, "Kitchen")

        assertSame(status, result)
        verify { sensorSettingsRepository.newDescription(SENSOR_ID, "Kitchen", any()) }
    }

    @Test
    fun `local description change is persisted without a network operation`() {
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns SensorSettings(id = SENSOR_ID)

        val result = interactor.updateDescriptionWithStatus(SENSOR_ID, null)

        assertNull(result)
        verify { sensorSettingsRepository.newDescription(SENSOR_ID, null, any()) }
        verify(exactly = 0) { networkInteractor.updateSensorSettingWithStatus(any(), any(), any(), any()) }
    }

    @Test
    fun `setting a network background deletes the previous file and uploads the new one`() {
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns SensorSettings(
            id = SENSOR_ID,
            userBackground = OLD_IMAGE,
            networkSensor = true,
        )

        interactor.setBackgroundImage(SENSOR_ID, NEW_IMAGE, uploadNow = true)

        verify(exactly = 1) { imageInteractor.deleteFile(OLD_IMAGE) }
        verify(exactly = 1) {
            sensorSettingsRepository.updateSensorBackground(SENSOR_ID, NEW_IMAGE, 0, null, any(), null)
        }
        verify(exactly = 1) { networkInteractor.uploadImage(SENSOR_ID, NEW_IMAGE, true) }
    }

    companion object {
        private const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
        private const val EMAIL = "user@example.com"
        private const val OLD_IMAGE = "file:///old.jpg"
        private const val NEW_IMAGE = "file:///new.jpg"
    }
}
