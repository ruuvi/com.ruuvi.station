package com.ruuvi.station.tagdetails.ui

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import org.junit.Assert.assertEquals
import org.junit.Test

class SensorCardLayoutTest {
    @Test
    fun `measurements are placed on the first pass and aligned to the viewport bottom`() {
        val result = measureCard(
            viewport = IntSize(400, 800),
            topSize = IntSize(200, 220),
            valuesSize = IntSize(400, 156),
        )

        assertEquals(IntSize(400, 800), result.size)
        assertEquals(IntOffset(100, 32), result.topPosition)
        assertEquals(IntOffset(0, 644), result.valuesPosition)
    }

    @Test
    fun `compact screens scroll overflowing measurements without overlap`() {
        val result = measureCard(
            viewport = IntSize(600, 300),
            topSize = IntSize(200, 220),
            valuesSize = IntSize(600, 200),
        )

        assertEquals(IntSize(600, 434), result.size)
        assertEquals(IntOffset(200, 8), result.topPosition)
        assertEquals(IntOffset(0, 234), result.valuesPosition)
    }

    @Test
    fun `tall primary measurements are not clipped to half the viewport`() {
        val result = measureCard(
            viewport = IntSize(400, 600),
            topSize = IntSize(300, 650),
            valuesSize = IntSize(400, 200),
        )

        assertEquals(IntSize(400, 888), result.size)
        assertEquals(IntOffset(50, 32), result.topPosition)
        assertEquals(IntOffset(0, 688), result.valuesPosition)
    }

    @Test
    fun `large measurement grids retain the primary measurement half-height area`() {
        val result = measureCard(
            viewport = IntSize(400, 700),
            topSize = IntSize(200, 300),
            valuesSize = IntSize(400, 720),
        )

        assertEquals(IntSize(400, 1070), result.size)
        assertEquals(IntOffset(0, 350), result.valuesPosition)
    }

    @Test
    fun `empty and single-measurement cards do not introduce scrolling`() {
        for (topSize in listOf(IntSize.Zero, IntSize(200, 220))) {
            val result = measureCard(
                viewport = IntSize(400, 600),
                topSize = topSize,
                valuesSize = IntSize.Zero,
            )

            assertEquals(IntSize(400, 600), result.size)
        }
    }

    @Test
    fun `compact padding switches at a four hundred dp viewport`() {
        for ((height, expectedPadding) in listOf(399 to 8, 400 to 32)) {
            val result = measureCard(
                viewport = IntSize(400, height),
                topSize = IntSize(200, 100),
                valuesSize = IntSize(400, 100),
            )

            assertEquals(expectedPadding, result.topPosition.y)
        }
    }

    @Test
    fun `padding uses display density and centering supports right to left layouts`() {
        val result = measureCard(
            viewport = IntSize(800, 1200),
            topSize = IntSize(440, 440),
            valuesSize = IntSize(800, 400),
            density = 2f,
            layoutDirection = LayoutDirection.Rtl,
        )

        assertEquals(IntSize(800, 1200), result.size)
        assertEquals(IntOffset(180, 64), result.topPosition)
        assertEquals(IntOffset(0, 800), result.valuesPosition)
    }

    private fun measureCard(
        viewport: IntSize,
        topSize: IntSize,
        valuesSize: IntSize,
        density: Float = 1f,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ): MeasuredCard {
        val scope = object : MeasureScope {
            override val density = density
            override val fontScale = 1f
            override val layoutDirection = layoutDirection
        }
        val top = TestMeasurable(topSize)
        val values = TestMeasurable(valuesSize)
        val result = with(sensorCardMeasurePolicy) {
            scope.measure(
                listOf(top, values),
                Constraints(
                    minWidth = viewport.width,
                    maxWidth = viewport.width,
                    minHeight = viewport.height,
                    maxHeight = Constraints.Infinity,
                ),
            )
        }
        result.placeChildren()

        assertEquals(1, top.measureCount)
        assertEquals(1, values.measureCount)
        return MeasuredCard(
            size = IntSize(result.width, result.height),
            topPosition = requireNotNull(top.placeable.position),
            valuesPosition = requireNotNull(values.placeable.position),
        )
    }

    private data class MeasuredCard(
        val size: IntSize,
        val topPosition: IntOffset,
        val valuesPosition: IntOffset,
    )

    private class TestMeasurable(private val size: IntSize) : Measurable {
        override val parentData: Any? = null
        var measureCount = 0
        lateinit var placeable: TestPlaceable

        override fun measure(constraints: Constraints): Placeable {
            measureCount++
            assertEquals(0, constraints.minHeight)
            assertEquals(Constraints.Infinity, constraints.maxHeight)
            return TestPlaceable(size, constraints).also { placeable = it }
        }

        override fun minIntrinsicWidth(height: Int): Int = error("Unexpected intrinsic measurement")
        override fun maxIntrinsicWidth(height: Int): Int = error("Unexpected intrinsic measurement")
        override fun minIntrinsicHeight(width: Int): Int = error("Unexpected intrinsic measurement")
        override fun maxIntrinsicHeight(width: Int): Int = error("Unexpected intrinsic measurement")
    }

    private class TestPlaceable(size: IntSize, constraints: Constraints) : Placeable() {
        var position: IntOffset? = null

        init {
            measurementConstraints = constraints
            measuredSize = IntSize(
                constraints.constrainWidth(size.width),
                constraints.constrainHeight(size.height),
            )
        }

        override fun get(alignmentLine: AlignmentLine): Int = AlignmentLine.Unspecified

        override fun placeAt(
            position: IntOffset,
            zIndex: Float,
            layerBlock: (GraphicsLayerScope.() -> Unit)?,
        ) {
            this.position = position
        }
    }
}
