package com.ruuvi.station.settings.ui

import android.content.Context
import com.ruuvi.station.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsRoutesTest {

    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        mockContext = mockk()
        every { mockContext.getString(R.string.settings_appearance) } returns "Appearance"
        every { mockContext.getString(R.string.menu_app_settings) } returns "App settings"
        every { mockContext.getString(R.string.settings_global_units) } returns "Units"
        every { mockContext.getString(R.string.settings_resolution) } returns "Resolution"
        every { mockContext.getString(R.string.settings_temperature) } returns "Temperature unit"
        every { mockContext.getString(R.string.settings_humidity) } returns "Humidity unit"
        every { mockContext.getString(R.string.settings_pressure) } returns "Pressure unit"
        every { mockContext.getString(R.string.settings_background_scan) } returns "Background scan"
        every { mockContext.getString(R.string.settings_chart) } returns "Chart settings"
        every { mockContext.getString(R.string.ruuvi_cloud) } returns "Ruuvi Cloud"
        every { mockContext.getString(R.string.settings_data_forwarding) } returns "Data forwarding"
        every { mockContext.getString(R.string.settings_alert_notifications) } returns "Alert notifications"
        every { mockContext.getString(R.string.settings_developer) } returns "Developer settings"
    }

    @Test
    fun `getTitleByRoute returns correct title for all routes`() {
        assertEquals("Appearance", SettingsRoutes.getTitleByRoute(mockContext, SettingsRoutes.APPEARANCE))
        assertEquals("App settings", SettingsRoutes.getTitleByRoute(mockContext, SettingsRoutes.LIST))
        assertEquals("Units", SettingsRoutes.getTitleByRoute(mockContext, SettingsRoutes.GLOBAL_UNITS))
        assertEquals("Units", SettingsRoutes.getTitleByRoute(mockContext, SettingsRoutes.GLOBAL_UNIT_SELECT))
        assertEquals("Resolution", SettingsRoutes.getTitleByRoute(mockContext, SettingsRoutes.RESOLUTION))
        assertEquals("Resolution", SettingsRoutes.getTitleByRoute(mockContext, SettingsRoutes.RESOLUTION_SELECT))
        assertEquals("Temperature unit", SettingsRoutes.getTitleByRoute(mockContext, SettingsRoutes.TEMPERATURE))
        assertEquals("Humidity unit", SettingsRoutes.getTitleByRoute(mockContext, SettingsRoutes.HUMIDITY))
        assertEquals("Pressure unit", SettingsRoutes.getTitleByRoute(mockContext, SettingsRoutes.PRESSURE))
        assertEquals("Background scan", SettingsRoutes.getTitleByRoute(mockContext, SettingsRoutes.BACKGROUNDSCAN))
        assertEquals("Chart settings", SettingsRoutes.getTitleByRoute(mockContext, SettingsRoutes.CHARTS))
        assertEquals("Ruuvi Cloud", SettingsRoutes.getTitleByRoute(mockContext, SettingsRoutes.CLOUD))
        assertEquals("Data forwarding", SettingsRoutes.getTitleByRoute(mockContext, SettingsRoutes.DATAFORWARDING))
        assertEquals("Alert notifications", SettingsRoutes.getTitleByRoute(mockContext, SettingsRoutes.ALERT_NOTIFICATIONS))
        assertEquals("Developer settings", SettingsRoutes.getTitleByRoute(mockContext, SettingsRoutes.DEVELOPER))
        assertEquals("App settings", SettingsRoutes.getTitleByRoute(mockContext, "unknown_route"))
    }

    @Test
    fun `globalUnitSelectRoute formats path correctly`() {
        assertEquals("global_unit_select/temperature", SettingsRoutes.globalUnitSelectRoute("temperature"))
    }

    @Test
    fun `resolutionSelectRoute formats path correctly`() {
        assertEquals("resolution_select/humidity", SettingsRoutes.resolutionSelectRoute("humidity"))
    }
}
