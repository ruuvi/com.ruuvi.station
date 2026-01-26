package com.ruuvi.station.network.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.image.ImageInteractor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field

/**
 * Unit tests for RuuviNetworkRepository URL switching behavior.
 *
 * Tests verify that:
 * - Production mode uses https://network.ruuvi.com/
 * - Development mode uses https://testnet.ruuvi.com/
 * - reinitialize() properly rebuilds Retrofit with the correct URL
 */
class RuuviNetworkRepositoryUrlTest {

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var imageInteractor: ImageInteractor

    @Before
    fun setUp() {
        preferencesRepository = mockk(relaxed = true)
        imageInteractor = mockk(relaxed = true)
    }

    // ==================== URL Constants Tests ====================

    @Test
    fun `BASE_URL constant has correct production value`() {
        val baseUrl = getCompanionConstant("BASE_URL")
        assertEquals("https://network.ruuvi.com/", baseUrl)
    }

    @Test
    fun `DEV_URL constant has correct development value`() {
        val devUrl = getCompanionConstant("DEV_URL")
        assertEquals("https://testnet.ruuvi.com/", devUrl)
    }

    @Test
    fun `URLs use HTTPS protocol`() {
        val baseUrl = getCompanionConstant("BASE_URL")
        val devUrl = getCompanionConstant("DEV_URL")
        assertTrue("Production URL should use HTTPS", baseUrl.startsWith("https://"))
        assertTrue("Dev URL should use HTTPS", devUrl.startsWith("https://"))
    }

    @Test
    fun `URLs end with trailing slash`() {
        val baseUrl = getCompanionConstant("BASE_URL")
        val devUrl = getCompanionConstant("DEV_URL")
        assertTrue("Production URL should end with /", baseUrl.endsWith("/"))
        assertTrue("Dev URL should end with /", devUrl.endsWith("/"))
    }

    // ==================== Retrofit URL Selection Tests ====================

    @Test
    fun `repository uses production URL when dev server is disabled`() {
        // Given
        every { preferencesRepository.isDevServerEnabled() } returns false

        // When
        val repository = RuuviNetworkRepository(
            Dispatchers.Unconfined,
            imageInteractor,
            preferencesRepository
        )

        // Then
        val retrofitBaseUrl = getRetrofitBaseUrl(repository)
        assertEquals("https://network.ruuvi.com/", retrofitBaseUrl)
    }

    @Test
    fun `repository uses dev URL when dev server is enabled`() {
        // Given
        every { preferencesRepository.isDevServerEnabled() } returns true

        // When
        val repository = RuuviNetworkRepository(
            Dispatchers.Unconfined,
            imageInteractor,
            preferencesRepository
        )

        // Then
        val retrofitBaseUrl = getRetrofitBaseUrl(repository)
        assertEquals("https://testnet.ruuvi.com/", retrofitBaseUrl)
    }

    @Test
    fun `reinitialize switches from production to dev URL`() {
        // Given - start with production
        every { preferencesRepository.isDevServerEnabled() } returns false
        val repository = RuuviNetworkRepository(
            Dispatchers.Unconfined,
            imageInteractor,
            preferencesRepository
        )
        assertEquals("https://network.ruuvi.com/", getRetrofitBaseUrl(repository))

        // When - switch to dev and reinitialize
        every { preferencesRepository.isDevServerEnabled() } returns true
        repository.reinitialize()

        // Then
        assertEquals("https://testnet.ruuvi.com/", getRetrofitBaseUrl(repository))
    }

    @Test
    fun `reinitialize switches from dev to production URL`() {
        // Given - start with dev
        every { preferencesRepository.isDevServerEnabled() } returns true
        val repository = RuuviNetworkRepository(
            Dispatchers.Unconfined,
            imageInteractor,
            preferencesRepository
        )
        assertEquals("https://testnet.ruuvi.com/", getRetrofitBaseUrl(repository))

        // When - switch to production and reinitialize
        every { preferencesRepository.isDevServerEnabled() } returns false
        repository.reinitialize()

        // Then
        assertEquals("https://network.ruuvi.com/", getRetrofitBaseUrl(repository))
    }

    @Test
    fun `reinitialize checks preferences for dev server state`() {
        // Given
        every { preferencesRepository.isDevServerEnabled() } returns false
        val repository = RuuviNetworkRepository(
            Dispatchers.Unconfined,
            imageInteractor,
            preferencesRepository
        )

        // When
        repository.reinitialize()

        // Then - should have checked preferences at least twice (init + reinitialize)
        verify(atLeast = 2) { preferencesRepository.isDevServerEnabled() }
    }

    // ==================== Helper Methods ====================

    private fun getCompanionConstant(fieldName: String): String {
        val companionClass = RuuviNetworkRepository::class.java
        val companionField = companionClass.getDeclaredField(fieldName)
        companionField.isAccessible = true
        return companionField.get(null) as String
    }

    private fun getRetrofitBaseUrl(repository: RuuviNetworkRepository): String {
        // Access the private retrofit field
        val retrofitField: Field = RuuviNetworkRepository::class.java.getDeclaredField("retrofit")
        retrofitField.isAccessible = true
        val retrofit = retrofitField.get(repository) as retrofit2.Retrofit
        return retrofit.baseUrl().toString()
    }
}
