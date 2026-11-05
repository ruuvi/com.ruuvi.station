package com.ruuvi.station.app.ui.components.modifier

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BottomFadingEdgeComposeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var scrollToEnd: () -> Unit
    private lateinit var scrollToMiddle: () -> Unit

    @Test fun `scrolling column fades only at bottom and restores final content`() = verifyScrollable("column")
    @Test fun `lazy list fades only at bottom and restores final content`() = verifyScrollable("list")
    @Test fun `lazy grid fades only at bottom and restores final content`() = verifyScrollable("grid")

    @Test fun `existing column fade handles the start middle and end`() = verifyExistingFade("column")
    @Test fun `existing list fade handles the start middle and end`() = verifyExistingFade("list")
    @Test fun `existing column fade leaves short content opaque`() = verifyExistingShortContent("column")
    @Test fun `existing list fade leaves short content opaque`() = verifyExistingShortContent("list")

    private fun verifyExistingFade(kind: String) {
        setContent(kind, existingFade = true)
        assertEquals(0f, capture()[50, 0].red, 0.01f)
        assertTrue(capture()[50, 199].red > 0.8f)
        compose.runOnIdle { scrollToMiddle() }
        assertTrue(capture()[50, 0].red > 0.8f)
        assertTrue(capture()[50, 199].red > 0.8f)
        compose.runOnIdle { scrollToEnd() }
        assertTrue(capture()[50, 0].red > 0.8f)
        assertEquals(0f, capture()[50, 199].red, 0.01f)
    }

    private fun verifyExistingShortContent(kind: String) {
        setContent(kind, count = 1, existingFade = true)
        assertEquals(0f, capture()[50, 0].red, 0.01f)
        assertEquals(0f, capture()[50, 49].red, 0.01f)
    }

    @Test
    fun `short content does not fade`() {
        setContent("column", count = 1)
        val pixels = capture()
        assertEquals(0f, pixels[50, 20].red, 0.01f)
        assertEquals(1f, pixels[50, 190].red, 0.01f)
    }

    @Test
    fun `zero length disables fade`() {
        setContent("column", length = 0.dp)
        assertEquals(0f, capture()[50, 199].red, 0.01f)
    }

    @Test
    fun `custom length and oversized length stay anchored to viewport`() {
        var length by mutableStateOf(24.dp)
        setContent("column", customLength = { length })
        assertEquals(0f, capture()[50, 175].red, 0.01f)
        assertTrue(capture()[50, 188].red in 0.05f..0.95f)
        compose.runOnIdle { length = 300.dp }
        assertTrue(capture()[50, 100].red in 0.05f..0.95f)
    }

    @Test
    fun `dark background is preserved while only content fades`() {
        setContent("column", background = Color.Black, foreground = Color.White)
        val pixels = capture()
        assertEquals(1f, pixels[50, 20].red, 0.01f)
        assertTrue(pixels[50, 176].red in 0.05f..0.95f)
        assertTrue(pixels[50, 199].red < pixels[50, 176].red)
    }

    @Test
    fun `column scroll state survives recreation of its composition`() {
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        lateinit var state: ScrollState
        restoration.setContent {
            state = rememberScrollState()
            val scope = rememberCoroutineScope()
            scrollToEnd = { scope.launch { state.scrollTo(75) } }
            Column(
                Modifier.size(100.dp, 200.dp).bottomFadingEdge(state).verticalScroll(state)
            ) { Box(Modifier.size(100.dp, 600.dp)) }
        }
        compose.runOnIdle { scrollToEnd() }
        compose.waitForIdle()
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle { assertEquals(75, state.value) }
    }

    private fun verifyScrollable(kind: String) {
        setContent(kind)
        val pixels = capture()
        assertEquals(0f, pixels[50, 20].red, 0.01f)
        assertEquals(0f, pixels[50, 151].red, 0.01f)
        assertTrue(pixels[50, 176].red in 0.05f..0.95f)
        assertTrue(pixels[50, 199].red > pixels[50, 176].red)
        compose.runOnIdle { scrollToEnd() }
        val finalPixels = capture()
        assertEquals(0f, finalPixels[50, 170].red, 0.01f)
        assertEquals(1f, finalPixels[50, 190].red, 0.01f)
    }

    private fun setContent(
        kind: String,
        count: Int = 12,
        length: Dp = 48.dp,
        customLength: (() -> Dp)? = null,
        background: Color = Color.White,
        foreground: Color = Color.Black,
        existingFade: Boolean = false,
    ) {
        compose.setContent {
            val scope = rememberCoroutineScope()
            val requestedLength = customLength?.invoke() ?: length
            Box(Modifier.size(100.dp, 200.dp).background(background).testTag("viewport")) {
                when (kind) {
                    "column" -> {
                        val state = rememberScrollState()
                        scrollToEnd = { scope.launch { state.scrollTo(state.maxValue) } }
                        scrollToMiddle = { scope.launch { state.scrollTo(100) } }
                        val fade = if (existingFade) Modifier.fadingEdge(state)
                            else Modifier.bottomFadingEdge(state, requestedLength)
                        Column(
                            Modifier.fillMaxSize().then(fade)
                                .verticalScroll(state).padding(bottom = if (existingFade) 0.dp else 24.dp)
                        ) { repeat(count) { Box(Modifier.fillMaxWidth().height(50.dp).background(foreground)) } }
                    }
                    "list" -> {
                        val state = rememberLazyListState()
                        scrollToEnd = { scope.launch { state.scrollToItem(count - 1) } }
                        scrollToMiddle = { scope.launch { state.scrollToItem(2) } }
                        val fade = if (existingFade) Modifier.fadingEdge(state)
                            else Modifier.bottomFadingEdge(state)
                        LazyColumn(
                            Modifier.fillMaxSize().then(fade),
                            state = state,
                            contentPadding = PaddingValues(bottom = if (existingFade) 0.dp else 24.dp),
                        ) { items(count) { Box(Modifier.fillMaxWidth().height(50.dp).background(foreground)) } }
                    }
                    "grid" -> {
                        val state = rememberLazyGridState()
                        scrollToEnd = { scope.launch { state.scrollToItem(count * 2 - 1) } }
                        LazyVerticalGrid(
                            GridCells.Fixed(2),
                            Modifier.fillMaxSize().bottomFadingEdge(state),
                            state = state,
                            contentPadding = PaddingValues(bottom = 24.dp),
                        ) { items(count * 2) { Box(Modifier.fillMaxWidth().height(50.dp).background(foreground)) } }
                    }
                }
            }
        }
    }

    private fun capture() = compose.onNodeWithTag("viewport").captureToImage().toPixelMap()
}
