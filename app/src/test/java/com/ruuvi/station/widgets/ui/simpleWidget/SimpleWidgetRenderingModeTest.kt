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
            secondaryColor = {
                resolvedThemeColor = true
                SECONDARY_COLOR
            },
        )

        assertNull(colors.value)
        assertNull(colors.secondary)
        assertFalse(resolvedThemeColor)
    }

    @Test
    fun `generated preview custom font colors are embedded`() {
        val colors = resolveSimpleWidgetEmbeddedColors(
            renderingMode = SimpleWidgetRenderingMode.GENERATED_PREVIEW,
            valueColor = { VALUE_COLOR },
            secondaryColor = { SECONDARY_COLOR },
        )

        assertEquals(VALUE_COLOR, colors.value)
        assertEquals(SECONDARY_COLOR, colors.secondary)
    }

    private companion object {
        const val VALUE_COLOR = 0x12345678
        const val SECONDARY_COLOR = 0x23456789
    }
}
