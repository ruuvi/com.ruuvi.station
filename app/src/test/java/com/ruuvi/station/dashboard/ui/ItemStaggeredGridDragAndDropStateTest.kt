package com.ruuvi.station.dashboard.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Geometry (mdpi, 1dp == 1px): each column is 100px wide, every item is 50px tall,
 * the grid viewport is 200px tall. In a single column item N spans y = N*50 .. N*50+50.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "mdpi")
class ItemStaggeredGridDragAndDropStateTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val items = mutableStateListOf<Int>()
    private val moves = mutableListOf<Pair<Int, Int>>()
    private var doneCount = 0
    private lateinit var state: ItemStaggeredGridDragAndDropState
    private lateinit var gridState: LazyStaggeredGridState

    private fun setContent(columns: Int = 1, count: Int = 10) {
        items.addAll(0 until count)
        compose.setContent {
            gridState = rememberLazyStaggeredGridState()
            val scope = rememberCoroutineScope()
            val overscrollJob = remember { mutableStateOf<Job?>(null) }
            state = rememberDragDropStaggeredGridState(
                lazyListState = gridState,
                onMove = { from, to ->
                    moves += from to to
                    val tmp = items[from]
                    items[from] = items[to]
                    items[to] = tmp
                },
                onDoneDragging = { doneCount++ }
            )
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(columns),
                state = gridState,
                modifier = Modifier
                    .size(width = (100 * columns).dp, height = 200.dp)
                    .testTag(GRID)
                    .dragGestureHandler(scope, state, overscrollJob)
            ) {
                items(items, key = { it }) {
                    Box(Modifier.fillMaxWidth().height(50.dp))
                }
            }
        }
        compose.waitForIdle()
    }

    private fun onState(block: ItemStaggeredGridDragAndDropState.() -> Unit) {
        compose.runOnIdle { state.block() }
        compose.waitForIdle()
    }

    private fun scrollTo(index: Int, offset: Int = 0) {
        compose.runOnIdle { runBlocking { gridState.scrollToItem(index, offset) } }
        compose.waitForIdle()
    }

    // region drag start

    @Test
    fun `drag start on an item selects it`() {
        setContent()
        onState { onDragStart(Offset(50f, 75f)) }

        assertTrue(state.isDragInProgress)
        assertEquals(1, state.getCurrentIndexOfDraggedListItem())
        assertEquals(IntOffset.Zero, state.elementDisplacement)
    }

    @Test
    fun `drag start outside items selects nothing and later drags are ignored`() {
        setContent(count = 2)
        onState { onDragStart(Offset(50f, 150f)) }

        assertTrue(state.isDragInProgress)
        assertEquals(-1, state.getCurrentIndexOfDraggedListItem())
        assertNull(state.elementDisplacement)

        onState { onDrag(Offset(0f, -100f)) }
        assertTrue(moves.isEmpty())
    }

    @Test
    fun `drag without drag start does nothing`() {
        setContent()
        onState { onDrag(Offset(0f, 100f)) }

        assertTrue(moves.isEmpty())
        assertFalse(state.isDragInProgress)
    }

    // endregion

    // region moving

    @Test
    fun `small drag only displaces the item`() {
        setContent()
        onState { onDragStart(Offset(50f, 25f)) }
        onState { onDrag(Offset(0f, 20f)) }

        assertTrue(moves.isEmpty())
        assertEquals(IntOffset(0, 20), state.elementDisplacement)
    }

    @Test
    fun `dragging the top item down keeps the grid scrolled to the top`() {
        setContent()
        onState { onDragStart(Offset(50f, 25f)) }
        onState { onDrag(Offset(0f, 60f)) }

        assertEquals(listOf(0 to 1), moves)
        assertEquals(listOf(1, 0), items.take(2))
        assertEquals(1, state.getCurrentIndexOfDraggedListItem())
        // Regression for #1756: the grid must not follow the dragged item's key.
        assertEquals(0, gridState.firstVisibleItemIndex)
        assertEquals(0, gridState.firstVisibleItemScrollOffset)
        // Dragged item now sits at y=50, finger moved 60 -> 10px displacement.
        assertEquals(IntOffset(0, 10), state.elementDisplacement)
    }

    @Test
    fun `dragging the first visible item down keeps a scrolled position`() {
        setContent()
        scrollTo(index = 2, offset = 10)
        // Item 2 now spans y = -10 .. 40, item 3 spans 40 .. 90.
        onState { onDragStart(Offset(50f, 20f)) }
        onState { onDrag(Offset(0f, 60f)) }

        assertEquals(listOf(2 to 3), moves)
        assertEquals(2, gridState.firstVisibleItemIndex)
        assertEquals(10, gridState.firstVisibleItemScrollOffset)
    }

    @Test
    fun `dragging an item up onto the first visible item keeps the scroll position`() {
        setContent()
        onState { onDragStart(Offset(50f, 75f)) }
        onState { onDrag(Offset(0f, -60f)) }

        assertEquals(listOf(1 to 0), moves)
        assertEquals(listOf(1, 0), items.take(2))
        assertEquals(0, gridState.firstVisibleItemIndex)
        assertEquals(0, gridState.firstVisibleItemScrollOffset)
    }

    @Test
    fun `dragging a middle item moves it without touching the scroll position`() {
        setContent()
        onState { onDragStart(Offset(50f, 75f)) }
        onState { onDrag(Offset(0f, 60f)) }

        assertEquals(listOf(1 to 2), moves)
        assertEquals(listOf(0, 2, 1), items.take(3))
        assertEquals(0, gridState.firstVisibleItemIndex)
    }

    @Test
    fun `consecutive drags move the item step by step`() {
        setContent()
        onState { onDragStart(Offset(50f, 25f)) }
        onState { onDrag(Offset(0f, 60f)) }
        onState { onDrag(Offset(0f, 50f)) }

        assertEquals(listOf(0 to 1, 1 to 2), moves)
        assertEquals(listOf(1, 2, 0), items.take(3))
        assertEquals(0, gridState.firstVisibleItemIndex)
    }

    @Test
    fun `dragging into a neighbour column moves the item`() {
        setContent(columns = 2)
        // Item 0 at x 0..100, item 1 at x 100..200, both on the first row.
        onState { onDragStart(Offset(50f, 25f)) }
        onState { onDrag(Offset(90f, -1f)) }

        assertEquals(listOf(0 to 1), moves)
    }

    @Test
    fun `barely entering a neighbour column does not move the item`() {
        setContent(columns = 2)
        onState { onDragStart(Offset(50f, 25f)) }
        onState { onDrag(Offset(50f, -1f)) }

        assertTrue(moves.isEmpty())
    }

    // endregion

    // region drag end

    @Test
    fun `finishing a drag resets state and reports done`() {
        setContent()
        onState { onDragStart(Offset(50f, 25f)) }
        onState { onDrag(Offset(0f, 60f)) }
        onState { onDragInterrupted(canceled = false) }

        assertEquals(1, doneCount)
        assertFalse(state.isDragInProgress)
        assertEquals(-1, state.getCurrentIndexOfDraggedListItem())
        assertNull(state.elementDisplacement)
        assertEquals(0f, state.checkForOverScroll())
    }

    @Test
    fun `cancelling a drag resets state without reporting done`() {
        setContent()
        onState { onDragStart(Offset(50f, 25f)) }
        onState { onDragInterrupted(canceled = true) }

        assertEquals(0, doneCount)
        assertFalse(state.isDragInProgress)
        assertEquals(-1, state.getCurrentIndexOfDraggedListItem())
    }

    // endregion

    // region overscroll

    @Test
    fun `no overscroll without a dragged item`() {
        setContent()
        assertEquals(0f, state.checkForOverScroll())
    }

    @Test
    fun `no overscroll before the item moves`() {
        setContent()
        onState { onDragStart(Offset(50f, 25f)) }
        assertEquals(0f, state.checkForOverScroll())
    }

    @Test
    fun `no overscroll while the item stays inside the viewport`() {
        setContent()
        onState { onDragStart(Offset(50f, 25f)) }
        onState { onDrag(Offset(0f, 10f)) }
        assertEquals(0f, state.checkForOverScroll())
    }

    @Test
    fun `dragging past the bottom edge asks to scroll down`() {
        setContent()
        onState { onDragStart(Offset(50f, 25f)) }
        onState { onDrag(Offset(0f, 180f)) }
        // Item bottom 50 + 180 = 230, viewport ends at 200.
        assertEquals(30f, state.checkForOverScroll())
    }

    @Test
    fun `dragging past the top edge asks to scroll up`() {
        setContent()
        scrollTo(index = 4)
        onState { onDragStart(Offset(50f, 25f)) }
        onState { onDrag(Offset(0f, -20f)) }
        assertEquals(-20f, state.checkForOverScroll())
    }

    // endregion

    // region gestures

    @Test
    fun `long press and drag reorders items and reports done`() {
        setContent()
        compose.onNodeWithTag(GRID).performTouchInput {
            down(Offset(50f, 25f))
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            repeat(6) { moveBy(Offset(0f, 10f)) }
            up()
        }
        compose.waitForIdle()

        assertEquals(listOf(0 to 1), moves)
        assertEquals(listOf(1, 0), items.take(2))
        assertEquals(1, doneCount)
        assertFalse(state.isDragInProgress)
        assertEquals(0, gridState.firstVisibleItemIndex)
    }

    @Test
    fun `dragging to the bottom edge auto scrolls the grid`() {
        setContent(count = 30)
        compose.onNodeWithTag(GRID).performTouchInput {
            down(Offset(50f, 125f))
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            repeat(20) { moveBy(Offset(0f, 10f)) }
            up()
        }
        compose.waitForIdle()

        assertTrue(moves.isNotEmpty())
        assertTrue(gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0)
        assertEquals(1, doneCount)
    }

    @Test
    fun `quick drag without long press does not start reordering`() {
        setContent()
        compose.onNodeWithTag(GRID).performTouchInput {
            down(Offset(50f, 25f))
            repeat(6) { moveBy(Offset(0f, 10f)) }
            up()
        }
        compose.waitForIdle()

        assertTrue(moves.isEmpty())
        assertEquals(0, doneCount)
    }

    // endregion

    @Test
    fun `visible item lookup returns null for indexes outside the visible range`() {
        setContent()
        compose.runOnIdle {
            assertNull(gridState.getVisibleItemInfoFor(-1))
            assertNull(gridState.getVisibleItemInfoFor(100))
            assertEquals(2, gridState.getVisibleItemInfoFor(2)?.index)
            assertEquals(gridState, state.getLazyListState())
        }
    }

    private companion object {
        const val GRID = "grid"
    }
}
