package com.ruuvi.station.dashboard.ui.dashboard_elements

import android.app.Application
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getAlignmentLinePosition
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BigValueExtDisplayTest {
    @get:Rule val compose = createComposeRule()

    private val value = EnvironmentValue(
        original = 0.8,
        value = 0.8,
        accuracy = Accuracy.Accuracy2,
        valueWithUnit = "0.80 g",
        valueWithoutUnit = "0.80",
        unitString = "g",
        unitType = UnitType.Acceleration.GForceX,
    )

    private val fontScale = mutableFloatStateOf(1f)
    private var contentSet = false

    @Test
    fun `measurement name box never overlaps the unit box`() {
        listOf(0.8f, 0.85f, 1f, 1.15f, 1.3f, 1.5f, 1.8f, 2f).forEach { scale ->
            setFontScale(scale)
            val unit = bounds("g")
            val title = bounds("Acc. X")
            assertTrue(
                "font scale $scale: title box top ${title.top} overlaps unit box bottom ${unit.bottom}",
                title.top >= unit.bottom,
            )
        }
    }

    @Test
    fun `measurement name stays close to the value baseline at default font scale`() {
        setFontScale(1f)
        assertEquals(baselineInRoot("0.80"), baselineInRoot("Acc. X"), 2f * compose.density.density)
    }

    @Test
    fun `measurement without a title keeps the unit beside the value in a scrolling row`() {
        compose.setContent {
            val density = Density(LocalDensity.current.density, 2f)
            CompositionLocalProvider(LocalDensity provides density) {
                RuuviTheme {
                    Box(Modifier.horizontalScroll(rememberScrollState())) {
                        BigValueExtDisplay(value = value, alertTriggered = true, showTitle = false)
                    }
                }
            }
        }

        compose.onNodeWithText("Acc. X").assertDoesNotExist()
        assertTrue(bounds("g").left > bounds("0.80").right)
    }

    @Test
    fun `AQI keeps the score unit and title separate above the progress indicator`() {
        assertAqiLayout(AQI.CalculatedAQI(62.0), expectedScore = "62", alertTriggered = true)
    }

    @Test
    fun `undefined AQI keeps the unit title and progress indicator with the placeholder`() {
        assertAqiLayout(AQI.UndefinedAQI, expectedScore = "-", alertTriggered = false)
    }

    private fun assertAqiLayout(aqi: AQI, expectedScore: String, alertTriggered: Boolean) {
        compose.setContent {
            val density = Density(LocalDensity.current.density, 2f)
            CompositionLocalProvider(LocalDensity provides density) {
                RuuviTheme {
                    AQIDisplay(
                        value = aqi,
                        alertTriggered = alertTriggered,
                        modifier = Modifier.width(160.dp).testTag("aqi"),
                    )
                }
            }
        }

        val score = bounds(expectedScore)
        val unit = bounds("/100")
        val title = bounds(RuntimeEnvironment.getApplication().getString(R.string.air_quality))
        val display = compose.onNodeWithTag("aqi").fetchSemanticsNode().boundsInRoot
        val textBottom = maxOf(score.bottom, unit.bottom, title.bottom)

        assertTrue(unit.left > score.right)
        assertTrue(title.top >= unit.bottom)
        assertTrue(title.right <= display.right)
        assertEquals(12f * compose.density.density, display.bottom - textBottom, 1f)
    }

    private fun setFontScale(scale: Float) {
        fontScale.floatValue = scale
        if (!contentSet) {
            contentSet = true
            compose.setContent {
                val density = Density(LocalDensity.current.density, fontScale.floatValue)
                CompositionLocalProvider(LocalDensity provides density) {
                    RuuviTheme {
                        BigValueExtDisplay(value = value, alertTriggered = false, showTitle = true)
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun bounds(text: String): Rect =
        compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot

    private fun baselineInRoot(text: String): Float {
        val node = compose.onNodeWithText(text)
        return node.fetchSemanticsNode().positionInRoot.y +
            node.getAlignmentLinePosition(FirstBaseline).value * compose.density.density
    }
}
