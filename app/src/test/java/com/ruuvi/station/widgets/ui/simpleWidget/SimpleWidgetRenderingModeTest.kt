package com.ruuvi.station.widgets.ui.simpleWidget

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

    private companion object {
        const val VALUE_COLOR = 0x12345678
        const val UNIT_COLOR = 0x23456789
    }
}
