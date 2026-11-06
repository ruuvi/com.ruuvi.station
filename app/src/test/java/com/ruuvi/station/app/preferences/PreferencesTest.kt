package com.ruuvi.station.app.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.text.util.LocalePreferences
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.ruuvi.station.units.model.UnitType.TemperatureUnit
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.After
import org.junit.Rule
import org.junit.Test

class PreferencesTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @After
    fun tearDown() {
        unmockkStatic(PreferenceManager::class)
    }

    @Test
    fun `uses the OS Fahrenheit preference as the default temperature unit`() {
        assertEquals(
            TemperatureUnit.Fahrenheit.unitCode,
            Preferences.defaultTemperatureUnitCode(
                LocalePreferences.TemperatureUnit.FAHRENHEIT
            )
        )
    }

    @Test
    fun `uses the OS Celsius preference as the default temperature unit`() {
        assertEquals(
            TemperatureUnit.Celsius.unitCode,
            Preferences.defaultTemperatureUnitCode(
                LocalePreferences.TemperatureUnit.CELSIUS
            )
        )
    }

    @Test
    fun `uses the OS Kelvin preference as the default temperature unit`() {
        assertEquals(
            TemperatureUnit.Kelvin.unitCode,
            Preferences.defaultTemperatureUnitCode(
                LocalePreferences.TemperatureUnit.KELVIN
            )
        )
    }

    @Test
    fun `refreshes the OS temperature default when LiveData becomes active again`() {
        var osDefault = TemperatureUnit.Celsius.unitCode
        val preferences = mockk<SharedPreferences>(relaxed = true)
        every { preferences.getString("temperature", any()) } answers { secondArg<String>() }
        val liveData = SharedPreferenceStringLiveData(
            preferences,
            "temperature",
            osDefault
        ) { osDefault }
        val values = mutableListOf<String>()
        val observer = Observer<String> { values.add(it) }

        liveData.observeForever(observer)
        liveData.removeObserver(observer)
        osDefault = TemperatureUnit.Fahrenheit.unitCode
        liveData.observeForever(observer)

        assertEquals(
            listOf(TemperatureUnit.Celsius.unitCode, TemperatureUnit.Fahrenheit.unitCode),
            values
        )
        liveData.removeObserver(observer)
    }

    @Test
    fun `uses the OS temperature default when no app unit is saved`() {
        val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
        every { sharedPreferences.getString("pref_temperature_unit", any()) } answers { secondArg<String>() }
        val preferences = preferencesUsing(sharedPreferences)
        val values = mutableListOf<String>()
        val observer = Observer<String> { values.add(it) }
        val liveData = preferences.getTemperatureUnitCodeLiveData()

        liveData.observeForever(observer)

        assertEquals(listOf(Preferences.defaultTemperatureUnitCode()), values)
        liveData.removeObserver(observer)
    }

    @Test
    fun `uses the saved temperature unit instead of the OS default`() {
        val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
        every {
            sharedPreferences.getString(
                "pref_temperature_unit",
                any()
            )
        } returns TemperatureUnit.Kelvin.unitCode
        val preferences = preferencesUsing(sharedPreferences)
        val values = mutableListOf<String>()
        val observer = Observer<String> { values.add(it) }
        val liveData = preferences.getTemperatureUnitCodeLiveData()

        liveData.observeForever(observer)

        assertEquals(listOf(TemperatureUnit.Kelvin.unitCode), values)
        liveData.removeObserver(observer)
    }

    @Test
    fun `dashboard card timeout defaults to sixty minutes`() {
        val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
        every {
            sharedPreferences.getLong("pref_dashboard_card_timeout_seconds", any())
        } answers { secondArg<Long>() }

        assertEquals(3_600L, preferencesUsing(sharedPreferences).dashboardCardTimeoutSeconds)
    }

    @Test
    fun `dashboard card timeout reads the saved long value`() {
        val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
        every {
            sharedPreferences.getLong("pref_dashboard_card_timeout_seconds", any())
        } returns Long.MAX_VALUE

        assertEquals(Long.MAX_VALUE, preferencesUsing(sharedPreferences).dashboardCardTimeoutSeconds)
    }

    @Test
    fun `dashboard card timeout persists the minimum and large custom values`() {
        val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { sharedPreferences.edit() } returns editor
        val preferences = preferencesUsing(sharedPreferences)

        for (seconds in listOf(1L, 137L, Long.MAX_VALUE)) {
            preferences.dashboardCardTimeoutSeconds = seconds

            verify { editor.putLong("pref_dashboard_card_timeout_seconds", seconds) }
        }
        verify(exactly = 3) { editor.apply() }
    }

    @Test
    fun `dashboard card timeout refuses values below one second`() {
        val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
        val preferences = preferencesUsing(sharedPreferences)

        for (seconds in listOf(0L, -1L)) {
            assertThrows(IllegalArgumentException::class.java) {
                preferences.dashboardCardTimeoutSeconds = seconds
            }
        }
        verify(exactly = 0) { sharedPreferences.edit() }
    }

    private fun preferencesUsing(sharedPreferences: SharedPreferences): Preferences {
        val context = mockk<Context>()
        mockkStatic(PreferenceManager::class)
        every { PreferenceManager.getDefaultSharedPreferences(context) } returns sharedPreferences
        return Preferences(context)
    }
}
