package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SimpleWidgetRenderingModeTest {
    @Test
    fun `runtime custom font colors are supplied by dynamic tints`() {
        var resolvedThemeColor = false

        val colors = resolveSimpleWidgetEmbeddedColors(
            renderingMode = SimpleWidgetRenderingMode.RUNTIME,
            valueColor = {
                resolvedThemeColor = true
                VALUE_COLOR
            },
            unitColor = {
                resolvedThemeColor = true
                UNIT_COLOR
            },
        )

        assertNull(colors.value)
        assertNull(colors.unit)
        assertFalse(resolvedThemeColor)
    }

    @Test
    fun `generated preview custom font colors are embedded`() {
        val colors = resolveSimpleWidgetEmbeddedColors(
            renderingMode = SimpleWidgetRenderingMode.GENERATED_PREVIEW,
            valueColor = { VALUE_COLOR },
            unitColor = { UNIT_COLOR },
        )

        assertEquals(VALUE_COLOR, colors.value)
        assertEquals(UNIT_COLOR, colors.unit)
    }

    @Test
    fun `mobile preview keeps existing uncapped value typography`() {
        val typography = resolveSimpleWidgetPreviewTypography(isTablet = false)

        assertEquals(0.65f, typography.scale)
        assertNull(typography.maximumValueFontSize)
    }

    @Test
    fun `tablet preview caps the sensor value font size`() {
        val typography = resolveSimpleWidgetPreviewTypography(isTablet = true)

        assertEquals(0.65f, typography.scale)
        assertEquals(40.sp, typography.maximumValueFontSize)
    }

    private companion object {
        const val VALUE_COLOR = 0x12345678
        const val UNIT_COLOR = 0x23456789
    }
}
