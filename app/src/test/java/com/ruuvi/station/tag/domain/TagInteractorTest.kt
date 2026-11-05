package com.ruuvi.station.tag.domain

import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dashboard.domain.SensorsSortingInteractor
import com.ruuvi.station.database.domain.TagRepository
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TagInteractorTest {
    private val tagRepository = mockk<TagRepository>()
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val alarmCheckInteractor = mockk<AlarmCheckInteractor>()
    private val sortingInteractor = SensorsSortingInteractor(preferencesRepository, mockk())
    private var currentAccount = "first-account"
    private val interactor = TagInteractor(
        tagRepository = tagRepository,
        sensorHistoryRepository = mockk(),
        sensorSettingsRepository = mockk(),
        preferencesRepository = preferencesRepository,
        alarmCheckInteractor = alarmCheckInteractor,
        tagSettingsInteractor = mockk(),
        sortingInteractor = sortingInteractor,
    )

    @Before
    fun setUp() {
        every { preferencesRepository.getUserEmail() } answers { currentAccount }
        every { preferencesRepository.getSortedSensors() } returns ""
        every { alarmCheckInteractor.getAlarmStatus(any()) } returns AlarmSensorStatus.NoAlarms
    }

    @Test
    fun `cache starts empty without querying sensors or alarms`() {
        assertTrue(interactor.getCachedTags().isEmpty())

        verify { tagRepository wasNot Called }
        verify { alarmCheckInteractor wasNot Called }
    }

    @Test
    fun `snapshot includes sorted sensors and computed alarm status without reloading`() {
        every { tagRepository.getFavoriteSensors() } returns listOf(SENSOR_A, SENSOR_B)
        every { preferencesRepository.getSortedSensors() } returns """["sensor-b","sensor-a"]"""
        every { alarmCheckInteractor.getAlarmStatus(SENSOR_B) } returns
            AlarmSensorStatus.NotTriggered(2)
        val expected = listOf(
            SENSOR_B.copy(alarmSensorStatus = AlarmSensorStatus.NotTriggered(2)),
            SENSOR_A.copy(alarmSensorStatus = AlarmSensorStatus.NoAlarms),
        )

        assertEquals(expected, interactor.getTags())
        assertEquals(expected, interactor.getCachedTags())

        verify(exactly = 1) { tagRepository.getFavoriteSensors() }
        verify(exactly = 1) { alarmCheckInteractor.getAlarmStatus(SENSOR_A) }
        verify(exactly = 1) { alarmCheckInteractor.getAlarmStatus(SENSOR_B) }
    }

    @Test
    fun `snapshot follows a changed dashboard order without reloading sensors`() {
        every { tagRepository.getFavoriteSensors() } returns listOf(SENSOR_A, SENSOR_B)
        interactor.getTags()
        every { preferencesRepository.getSortedSensors() } returns """["sensor-b","sensor-a"]"""

        assertEquals(
            listOf(SENSOR_B.id, SENSOR_A.id),
            interactor.getCachedTags().map { it.id },
        )
        verify(exactly = 1) { tagRepository.getFavoriteSensors() }
    }

    @Test
    fun `clearing a custom order restores the default order in the snapshot`() {
        every { tagRepository.getFavoriteSensors() } returns listOf(SENSOR_A, SENSOR_B)
        every { preferencesRepository.getSortedSensors() } returns """["sensor-b","sensor-a"]"""
        interactor.getTags()

        every { preferencesRepository.getSortedSensors() } returns ""

        assertEquals(
            listOf(SENSOR_A.id, SENSOR_B.id),
            interactor.getCachedTags().map { it.id },
        )
        verify(exactly = 1) { tagRepository.getFavoriteSensors() }
    }

    @Test
    fun `a fresh read replaces the snapshot including an empty sensor list`() {
        every { tagRepository.getFavoriteSensors() } returnsMany listOf(
            listOf(SENSOR_A, SENSOR_B),
            listOf(SENSOR_B),
            emptyList(),
        )

        interactor.getTags()
        assertEquals(listOf(SENSOR_A.id, SENSOR_B.id), interactor.getCachedTags().map { it.id })
        interactor.getTags()
        assertEquals(listOf(SENSOR_B.id), interactor.getCachedTags().map { it.id })
        interactor.getTags()
        assertTrue(interactor.getCachedTags().isEmpty())
    }

    @Test
    fun `another account cannot reuse the previous account snapshot`() {
        every { tagRepository.getFavoriteSensors() } returns listOf(SENSOR_A)
        interactor.getTags()

        currentAccount = "second-account"

        assertTrue(interactor.getCachedTags().isEmpty())
        verify(exactly = 1) { tagRepository.getFavoriteSensors() }

        every { tagRepository.getFavoriteSensors() } returns listOf(SENSOR_B)
        interactor.getTags()
        assertEquals(listOf(SENSOR_B.id), interactor.getCachedTags().map { it.id })
    }

    @Test
    fun `signing out prevents reuse of the signed in snapshot`() {
        every { tagRepository.getFavoriteSensors() } returns listOf(SENSOR_A)
        interactor.getTags()

        currentAccount = ""

        assertTrue(interactor.getCachedTags().isEmpty())
        verify(exactly = 1) { tagRepository.getFavoriteSensors() }
    }

    @Test
    fun `an account change during a read invalidates the startup snapshot`() {
        every { tagRepository.getFavoriteSensors() } answers {
            currentAccount = "second-account"
            listOf(SENSOR_A)
        }

        interactor.getTags()

        assertTrue(interactor.getCachedTags().isEmpty())
    }

    private companion object {
        val SENSOR_A = ruuviTagPreview.copy(id = "sensor-a")
        val SENSOR_B = ruuviTagPreview.copy(id = "sensor-b")
    }
}
