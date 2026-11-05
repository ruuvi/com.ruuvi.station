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
import org.junit.Assert.assertEquals
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
    fun `stores marketing consent prompt history separately for each account`() {
        val sharedPreferences = inMemoryPreferences()
        val preferences = preferencesUsing(sharedPreferences)

        preferences.setMarketingConsentPromptLastShown("first@example.com", 1_000L)
        preferences.setMarketingConsentPromptLastShown("second@example.com", 2_000L)

        assertEquals(1_000L, preferences.getMarketingConsentPromptLastShown("first@example.com"))
        assertEquals(2_000L, preferences.getMarketingConsentPromptLastShown("second@example.com"))
    }

    @Test
    fun `migrates the legacy marketing consent prompt history to the active account`() {
        val sharedPreferences = inMemoryPreferences(
            mapOf("pref_marketing_consent_prompt_last_shown" to 1_000L)
        )
        val preferences = preferencesUsing(sharedPreferences)

        assertEquals(1_000L, preferences.getMarketingConsentPromptLastShown("first@example.com"))
        assertEquals(0L, preferences.getMarketingConsentPromptLastShown("second@example.com"))
    }

    private fun preferencesUsing(sharedPreferences: SharedPreferences): Preferences {
        val context = mockk<Context>()
        mockkStatic(PreferenceManager::class)
        every { PreferenceManager.getDefaultSharedPreferences(context) } returns sharedPreferences
        return Preferences(context)
    }

    private fun inMemoryPreferences(
        initialValues: Map<String, Long> = emptyMap()
    ): SharedPreferences {
        val values = initialValues.toMutableMap<String, Long>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val preferences = mockk<SharedPreferences>(relaxed = true)

        every { preferences.contains(any()) } answers { values.containsKey(firstArg()) }
        every { preferences.getLong(any(), any()) } answers {
            values[firstArg()] ?: secondArg()
        }
        every { preferences.edit() } returns editor
        every { editor.putLong(any(), any()) } answers {
            values[firstArg()] = secondArg()
            editor
        }
        every { editor.remove(any()) } answers {
            values.remove(firstArg())
            editor
        }
        return preferences
    }
}
