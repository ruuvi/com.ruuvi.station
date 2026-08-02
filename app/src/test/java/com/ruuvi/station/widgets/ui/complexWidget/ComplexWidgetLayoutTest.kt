package com.ruuvi.station.widgets.ui.complexWidget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ruuvi.station.widgets.ui.glance.WidgetRefreshButtonDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

class ComplexWidgetLayoutTest {
    @Test
    fun `narrow widget uses one measurement column`() {
        assertEquals(1, complexWidgetMeasurementColumns(40.dp))
        assertEquals(1, complexWidgetMeasurementColumns(110.dp, fontScale = 0.85f))
    }

    @Test
    fun `three cell widget uses two columns with small and normal fonts`() {
        assertEquals(2, complexWidgetMeasurementColumns(180.dp, fontScale = 0.85f))
        assertEquals(2, complexWidgetMeasurementColumns(180.dp))
        assertEquals(1, complexWidgetMeasurementColumns(180.dp, fontScale = 1.15f))
    }

    @Test
    fun `four cell widget uses three columns only with small font`() {
        assertEquals(3, complexWidgetMeasurementColumns(280.dp, fontScale = 0.85f))
        assertEquals(2, complexWidgetMeasurementColumns(280.dp))
        assertEquals(2, complexWidgetMeasurementColumns(280.dp, fontScale = 1.15f))
    }

    @Test
    fun `five cell widget uses three columns with small and normal fonts`() {
        assertEquals(3, complexWidgetMeasurementColumns(350.dp, fontScale = 0.85f))
        assertEquals(3, complexWidgetMeasurementColumns(350.dp))
        assertEquals(2, complexWidgetMeasurementColumns(350.dp, fontScale = 1.15f))
    }

    @Test
    fun `wider widgets can add more than three columns`() {
        assertEquals(4, complexWidgetMeasurementColumns(420.dp))
        assertEquals(5, complexWidgetMeasurementColumns(520.dp))
        assertEquals(6, complexWidgetMeasurementColumns(620.dp))
    }

    @Test
    fun `column count does not exceed available measurements`() {
        assertEquals(3, complexWidgetMeasurementColumns(620.dp, maximumColumns = 3))
        assertEquals(1, complexWidgetMeasurementColumns(620.dp, maximumColumns = 1))
    }

    @Test
    fun `maximum accessibility font uses one or two columns`() {
        assertEquals(1, complexWidgetMeasurementColumns(280.dp, fontScale = 2f))
        assertEquals(2, complexWidgetMeasurementColumns(360.dp, fontScale = 2f))
    }

    @Test
    fun `font adjusted breakpoints preserve fixed padding and gutters`() {
        assertEquals(219.6f, complexWidgetRequiredWidth(2, fontScale = 1.3f).value, 0.001f)
        assertEquals(391f, complexWidgetRequiredWidth(3, fontScale = 1.3f).value, 0.001f)
        assertEquals(517.3333f, complexWidgetRequiredWidth(4, fontScale = 1.3f).value, 0.001f)
    }

    @Test
    fun `invalid font scale falls back to normal layout`() {
        assertEquals(3, complexWidgetMeasurementColumns(316.dp, fontScale = Float.NaN))
        assertEquals(3, complexWidgetMeasurementColumns(316.dp, fontScale = 0f))
    }

    @Test
    fun `invalid width falls back to one measurement column`() {
        assertEquals(1, complexWidgetMeasurementColumns(Dp.Unspecified))
    }

    @Test
    fun `all widget widths retain regular padding and spacing`() {
        listOf(40.dp, 100.dp, 180.dp, 280.dp).forEach { width ->
            val layout = complexWidgetLayoutConfig(width)

            assertEquals(4.dp, layout.outerPadding)
            assertEquals(8.dp, layout.horizontalPadding)
            assertEquals(8.dp, layout.headerTopPadding)
            assertEquals(12.dp, layout.headerBottomSpacing)
            assertEquals(3.dp, layout.rowVerticalPadding)
            assertEquals(8.dp, layout.footerTopSpacing)
            assertEquals(8.dp, layout.footerBottomPadding)
            assertEquals(12.dp, layout.measurementColumnSpacing)
            assertEquals(2.dp, layout.measurementUnitSpacing)
            assertEquals(4.dp, layout.measurementDescriptionSpacing)
        }
    }

    @Test
    fun `inter-column gutter stays fixed as the column count changes`() {
        assertEquals(12.dp, complexWidgetLayoutConfig(40.dp).measurementColumnSpacing)
        assertEquals(12.dp, complexWidgetLayoutConfig(180.dp).measurementColumnSpacing)
        assertEquals(12.dp, complexWidgetLayoutConfig(280.dp).measurementColumnSpacing)
    }

    @Test
    fun `only measurement column count changes with width`() {
        val layouts = listOf(40.dp, 100.dp, 180.dp, 280.dp)
            .map { complexWidgetLayoutConfig(it) }
        val normalizedLayouts = layouts.map { it.copy(measurementColumns = 0) }

        normalizedLayouts.drop(1).forEach {
            assertEquals(normalizedLayouts.first(), it)
        }
    }

    @Test
    fun `timestamp reserves only the refresh backing beyond existing end padding`() {
        val layout = complexWidgetLayoutConfig(180.dp)
        val contentEndInset = layout.outerPadding + layout.horizontalPadding

        val reservation = calculateComplexTimestampEndReservation(
            contentEndInset = contentEndInset,
            refreshBackingEndInset = WidgetRefreshButtonDefaults.backingEndInset
        )

        assertEquals(22.dp, reservation)
        assertEquals(
            WidgetRefreshButtonDefaults.backingEndInset,
            contentEndInset + reservation
        )
    }

    @Test
    fun `one cell timestamp gains width and stops at refresh backing`() {
        val widgetWidth = 82.dp
        val contentStartInset = 16.dp
        val contentEndInset = 16.dp
        val previousReservation = WidgetRefreshButtonDefaults.visualEndInset
        val reservation = calculateComplexTimestampEndReservation(
            contentEndInset = contentEndInset,
            refreshBackingEndInset = WidgetRefreshButtonDefaults.backingEndInset
        )

        val previousTimestampWidth =
            widgetWidth - contentStartInset - contentEndInset - previousReservation
        val timestampWidth =
            widgetWidth - contentStartInset - contentEndInset - reservation
        val timestampEnd = contentStartInset + timestampWidth
        val refreshBackingStart =
            widgetWidth - WidgetRefreshButtonDefaults.backingEndInset

        assertEquals(20.dp, previousTimestampWidth)
        assertEquals(32.dp, timestampWidth)
        assertEquals(refreshBackingStart, timestampEnd)
    }

    @Test
    fun `timestamp needs no extra reservation when content padding clears backing`() {
        assertEquals(
            0.dp,
            calculateComplexTimestampEndReservation(
                contentEndInset = 48.dp,
                refreshBackingEndInset = WidgetRefreshButtonDefaults.backingEndInset
            )
        )
    }

    @Test
    fun `smaller text receives padding needed to share reference baseline`() {
        assertEquals(
            4.dp,
            calculateBaselineTopPadding(
                referenceBaselineOffset = 16.dp,
                textBaselineOffset = 12.dp
            )
        )
    }

    @Test
    fun `text below reference baseline is not shifted upward`() {
        assertEquals(
            0.dp,
            calculateBaselineTopPadding(
                referenceBaselineOffset = 12.dp,
                textBaselineOffset = 16.dp
            )
        )
    }
}
