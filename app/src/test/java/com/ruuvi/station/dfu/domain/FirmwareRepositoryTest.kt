package com.ruuvi.station.dfu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.lang.reflect.Field

/**
 * Unit tests for FirmwareRepository URL behavior.
 *
 * FirmwareRepository should respect the dev server setting and use 
 * testnet.ruuvi.com when in dev mode, matching RuuviNetworkRepository behavior.
 */
class FirmwareRepositoryTest {

    companion object {
        private const val PROD_URL = "https://network.ruuvi.com/"
        private const val DEV_URL = "https://testnet.ruuvi.com/"
    }

    @Test
    fun `FirmwareRepository should use dev URL when dev server is enabled`() {
        val repository = FirmwareRepository()
        val retrofitBaseUrl = getRetrofitBaseUrl(repository)

        // FirmwareRepository should use PreferencesRepository.isDevServerEnabled() 
        // like RuuviNetworkRepository does
        if (retrofitBaseUrl == PROD_URL) {
            fail(
                "FirmwareRepository does not support dev server switching.\n" +
                "Expected: Should use $DEV_URL when dev mode is enabled\n" +
                "Actual: Always uses hardcoded $PROD_URL\n" +
                "Fix: Refactor FirmwareRepository to accept PreferencesRepository and implement " +
                "the same URL switching pattern as RuuviNetworkRepository"
            )
        }
    }

    @Test
    fun `FirmwareRepository should have reinitialize method for URL switching`() {
        val repository = FirmwareRepository()
        
        val hasReinitialize = try {
            repository.javaClass.getMethod("reinitialize")
            true
        } catch (e: NoSuchMethodException) {
            false
        }

        if (!hasReinitialize) {
            fail(
                "FirmwareRepository is missing reinitialize() method.\n" +
                "This method is needed to rebuild Retrofit when dev server setting changes.\n" +
                "See RuuviNetworkRepository.reinitialize() for reference implementation."
            )
        }
    }

    @Test
    fun `FirmwareRepository should accept PreferencesRepository dependency`() {
        val constructors = FirmwareRepository::class.java.constructors
        val hasPreferencesParam = constructors.any { constructor ->
            constructor.parameterTypes.any { 
                it.simpleName == "PreferencesRepository" 
            }
        }

        if (!hasPreferencesParam) {
            fail(
                "FirmwareRepository does not accept PreferencesRepository as a dependency.\n" +
                "This is needed to check isDevServerEnabled() for URL switching.\n" +
                "Current constructor: FirmwareRepository()\n" +
                "Expected: FirmwareRepository(preferencesRepository: PreferencesRepository)"
            )
        }
    }

    // ==================== Helper Methods ====================

    private fun getRetrofitBaseUrl(repository: FirmwareRepository): String {
        val retrofitField: Field = FirmwareRepository::class.java.getDeclaredField("retrofit")
        retrofitField.isAccessible = true
        val retrofit = retrofitField.get(repository) as retrofit2.Retrofit
        return retrofit.baseUrl().toString()
    }
}
