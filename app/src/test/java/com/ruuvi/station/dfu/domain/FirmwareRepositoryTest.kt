package com.ruuvi.station.dfu.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dfu.data.FirmwareData
import com.ruuvi.station.dfu.data.FirmwareInfo
import com.ruuvi.station.dfu.data.FirmwareResponse
import com.ruuvi.station.dfu.ui.FirmwareVersionOption
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FirmwareRepository business logic.
 *
 * Tests verify that:
 * - Repository uses correct server URL based on dev mode setting
 * - getLatest() returns latest firmware when API succeeds
 * - getLatest() returns null when API fails or returns error
 * - getOptions() excludes beta when version matches latest
 * - getOptions() excludes alpha when version matches latest
 * - getOptions() includes all versions when they differ
 * 
 * Note: Alpha and beta firmware versions are only available from the dev server
 * (testnet.ruuvi.com). The production server (network.ruuvi.com) only returns
 * the latest stable version.
 */
class FirmwareRepositoryTest {

    private lateinit var mockApi: FirmwareApi
    private lateinit var mockPreferencesRepository: PreferencesRepository
    private lateinit var repository: FirmwareRepository

    private val sampleFirmware = FirmwareInfo(
        version = "3.30.0",
        url = "https://example.com/firmware.zip",
        created_at = "2026-01-01",
        versionCode = 330,
        fileName = "ruuvi_firmware.zip",
        fwloader = "fwloader.zip",
        mcuboot_s1 = "mcuboot_s1.zip",
        mcuboot = "mcuboot.zip"
    )

    @Before
    fun setUp() {
        mockApi = mockk()
        mockPreferencesRepository = mockk()
        every { mockPreferencesRepository.getServerUrl() } returns PreferencesRepository.PROD_URL
        repository = FirmwareRepository(mockPreferencesRepository, mockApi)
    }


    @Test
    fun `repository queries PreferencesRepository for server URL on reinitialize`() {
        // Given
        every { mockPreferencesRepository.getServerUrl() } returns PreferencesRepository.PROD_URL

        // Then - not called during setUp (test constructor uses mock api directly)
        verify(exactly = 0) { mockPreferencesRepository.getServerUrl() }

        // When
        repository.reinitialize()

        // Then - called once during reinitialize
        verify(exactly = 1) { mockPreferencesRepository.getServerUrl() }
    }

    // ==================== getLatest() Tests ====================

    @Test
    fun `getLatest returns firmware when API returns success`() = runTest {
        // Given
        coEvery { mockApi.getFirmware() } returns FirmwareResponse(
            result = "success",
            data = FirmwareData(latest = sampleFirmware, beta = null, alpha = null)
        )

        // When
        val result = repository.getLatest()

        // Then
        assertNotNull(result)
        assertEquals("3.30.0", result?.version)
    }

    @Test
    fun `getLatest returns null when API returns non-success result`() = runTest {
        // Given
        coEvery { mockApi.getFirmware() } returns FirmwareResponse(
            result = "error",
            data = FirmwareData(latest = null, beta = null, alpha = null)
        )

        // When
        val result = repository.getLatest()

        // Then
        assertNull(result)
    }

    @Test
    fun `getLatest returns null when API throws exception`() = runTest {
        // Given
        coEvery { mockApi.getFirmware() } throws RuntimeException("Network error")

        // When
        val result = repository.getLatest()

        // Then
        assertNull(result)
    }

    // ==================== getOptions() Tests ====================

    @Test
    fun `getOptions excludes beta when version matches latest`() = runTest {
        // Given - beta has same version as latest
        val latest = sampleFirmware.copy(version = "3.30.0")
        val beta = sampleFirmware.copy(version = "3.30.0") // Same version

        coEvery { mockApi.getFirmware() } returns FirmwareResponse(
            result = "success",
            data = FirmwareData(latest = latest, beta = beta, alpha = null)
        )

        // When
        val result = repository.getOptions()

        // Then
        assertNotNull(result)
        assertEquals(1, result?.size)
        assertTrue(result?.first() is FirmwareVersionOption.Latest)
    }

    @Test
    fun `getOptions excludes alpha when version matches latest`() = runTest {
        // Given - alpha has same version as latest
        val latest = sampleFirmware.copy(version = "3.30.0")
        val alpha = sampleFirmware.copy(version = "3.30.0") // Same version

        coEvery { mockApi.getFirmware() } returns FirmwareResponse(
            result = "success",
            data = FirmwareData(latest = latest, beta = null, alpha = alpha)
        )

        // When
        val result = repository.getOptions()

        // Then
        assertNotNull(result)
        assertEquals(1, result?.size)
        assertTrue(result?.first() is FirmwareVersionOption.Latest)
    }

    @Test
    fun `getOptions includes beta when version differs from latest`() = runTest {
        // Given - beta has different version
        val latest = sampleFirmware.copy(version = "3.30.0")
        val beta = sampleFirmware.copy(version = "3.31.0-beta") // Different version

        coEvery { mockApi.getFirmware() } returns FirmwareResponse(
            result = "success",
            data = FirmwareData(latest = latest, beta = beta, alpha = null)
        )

        // When
        val result = repository.getOptions()

        // Then
        assertNotNull(result)
        assertEquals(2, result?.size)
        assertTrue(result?.any { it is FirmwareVersionOption.Latest } == true)
        assertTrue(result?.any { it is FirmwareVersionOption.Beta } == true)
    }

    @Test
    fun `getOptions includes alpha when version differs from latest`() = runTest {
        // Given - alpha has different version
        val latest = sampleFirmware.copy(version = "3.30.0")
        val alpha = sampleFirmware.copy(version = "3.32.0-alpha") // Different version

        coEvery { mockApi.getFirmware() } returns FirmwareResponse(
            result = "success",
            data = FirmwareData(latest = latest, beta = null, alpha = alpha)
        )

        // When
        val result = repository.getOptions()

        // Then
        assertNotNull(result)
        assertEquals(2, result?.size)
        assertTrue(result?.any { it is FirmwareVersionOption.Latest } == true)
        assertTrue(result?.any { it is FirmwareVersionOption.Alpha } == true)
    }

    @Test
    fun `getOptions includes all versions when they all differ`() = runTest {
        // Given - all versions are different
        val latest = sampleFirmware.copy(version = "3.30.0")
        val beta = sampleFirmware.copy(version = "3.31.0-beta")
        val alpha = sampleFirmware.copy(version = "3.32.0-alpha")

        coEvery { mockApi.getFirmware() } returns FirmwareResponse(
            result = "success",
            data = FirmwareData(latest = latest, beta = beta, alpha = alpha)
        )

        // When
        val result = repository.getOptions()

        // Then
        assertNotNull(result)
        assertEquals(3, result?.size)
        assertTrue(result?.any { it is FirmwareVersionOption.Latest } == true)
        assertTrue(result?.any { it is FirmwareVersionOption.Beta } == true)
        assertTrue(result?.any { it is FirmwareVersionOption.Alpha } == true)
    }

    @Test
    fun `getOptions returns null when API fails`() = runTest {
        // Given
        coEvery { mockApi.getFirmware() } throws RuntimeException("Network error")

        // When
        val result = repository.getOptions()

        // Then
        assertNull(result)
    }

    @Test
    fun `getOptions returns null when API returns non-success`() = runTest {
        // Given
        coEvery { mockApi.getFirmware() } returns FirmwareResponse(
            result = "error",
            data = FirmwareData(latest = null, beta = null, alpha = null)
        )

        // When
        val result = repository.getOptions()

        // Then
        assertNull(result)
    }
}
