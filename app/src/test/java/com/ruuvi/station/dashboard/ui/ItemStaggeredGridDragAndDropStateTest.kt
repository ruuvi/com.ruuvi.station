package com.ruuvi.station.dashboard.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
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

    /** [itemHeight] is per item (not per slot), so a card keeps its height when it is moved. */
    private fun setContent(columns: Int = 1, count: Int = 10, itemHeight: (item: Int) -> Int = { 50 }) {
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
                itemsIndexed(items, key = { index, item -> state.itemKey(index, item) }) { _, item ->
                    Box(Modifier.fillMaxWidth().height(itemHeight(item).dp))
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
    fun `dragging the first visible card in a scrolled uneven grid keeps the other column in place`() {
        // Regression for #1756: restoring a single index and offset reset the other column's
        // offset, so its top card jumped (e.g. from y = -15 to y = -45) when the first visible
        // card was dragged.
        // Uneven cards; the two that swap (3 and 6, both in the left column) are the same height so
        // the swap itself does not reflow any other card.
        val heights = listOf(70, 40, 55, 80, 45, 90, 80, 50, 75, 65, 85, 40, 70, 55, 60, 80)
        setContent(columns = 2, count = heights.size, itemHeight = { heights[it] })
        compose.runOnIdle { runBlocking { gridState.scrollBy(135f) } }
        compose.waitForIdle()

        val before = compose.runOnIdle { gridState.layoutInfo.visibleItemsInfo.toList() }
        val first = before.minBy { it.index }
        val otherColumnTop = before.filter { it.offset.x != first.offset.x }.minBy { it.offset.y }
        // The columns are scrolled by different amounts, which is what made the bug visible.
        assertTrue(first.offset.y != otherColumnTop.offset.y)
        val target = before.filter { it.offset.x == first.offset.x && it.index > first.index }.minBy { it.index }
        assertEquals(first.size.height, target.size.height)

        // Grab the card just above its bottom edge and drag until that edge passes the target's
        // bottom; the finger then rests inside the target.
        val start = Offset(first.offset.x + 10f, (first.offset.y + first.size.height - 2).toFloat())
        val distance = (target.offset.y + target.size.height - first.offset.y - first.size.height + 1).toFloat()
        onState { onDragStart(start) }
        onState { onDrag(Offset(0f, distance)) }

        assertEquals(listOf(first.index to target.index), moves)
        val after = compose.runOnIdle { gridState.layoutInfo.visibleItemsInfo.toList() }
        // Cards are matched by index: a swap leaves every other card's index unchanged.
        val otherColumnTopAfter = after.firstOrNull { it.index == otherColumnTop.index }
        assertEquals(otherColumnTop.offset, otherColumnTopAfter?.offset)
        // No card other than the two that swapped moves, in either column.
        val movedIndices = setOf(first.index, target.index)
        before.filter { it.index !in movedIndices }.forEach { card ->
            assertEquals("card ${card.index}", card.offset, after.firstOrNull { it.index == card.index }?.offset)
        }

        // Dropping the card brings back the real keys; the grid still does not move.
        onState { onDragInterrupted(false) }
        val dropped = compose.runOnIdle { gridState.layoutInfo.visibleItemsInfo.toList() }
        after.forEach { card ->
            assertEquals("card ${card.index} after drop", card.offset, dropped.firstOrNull { it.index == card.index }?.offset)
        }
        assertEquals(1, doneCount)
    }

    /**
     * Drags the first visible card onto its neighbour in the same column (down onto the next one,
     * or the next one up onto it) and returns where the other column's top card was before and
     * after, or null when this scroll position has no such pair.
     */
    private fun otherColumnTopAroundFirstCardDrag(scroll: Int, down: Boolean): Pair<IntOffset, IntOffset?>? {
        scrollTo(index = 0)
        compose.runOnIdle { runBlocking { gridState.scrollBy(scroll.toFloat()) } }
        compose.waitForIdle()
        moves.clear()

        val before = compose.runOnIdle { gridState.layoutInfo.visibleItemsInfo.toList() }
        val first = before.minByOrNull { it.index } ?: return null
        val otherColumn = before.filter { it.offset.x != first.offset.x }
        val otherColumnTop = otherColumn.minByOrNull { it.offset.y } ?: return null
        val next = before.filter { it.offset.x == first.offset.x && it.index > first.index }
            .minByOrNull { it.index } ?: return null
        // Only the case from the bug report: the columns are scrolled by different amounts.
        if (first.offset.y == otherColumnTop.offset.y) return null

        val (dragged, target) = if (down) first to next else next to first
        val start: Offset
        val distance: Float
        if (down) {
            // Grab just above the bottom edge; drag until it passes the target's bottom edge.
            start = Offset(dragged.offset.x + 10f, (dragged.offset.y + dragged.size.height - 2).toFloat())
            distance = (target.offset.y + target.size.height - dragged.offset.y - dragged.size.height + 1).toFloat()
        } else {
            // Grab just below the top edge; drag until it passes the target's top edge.
            start = Offset(dragged.offset.x + 10f, (dragged.offset.y + 2).toFloat())
            distance = (target.offset.y - dragged.offset.y - 1).toFloat()
        }
        // The grab point must be on screen for a real finger.
        if (start.y < 0) return null
        onState { onDragStart(start) }
        onState { onDrag(Offset(0f, distance)) }
        assertEquals("scroll $scroll", listOf(dragged.index to target.index), moves)

        val after = compose.runOnIdle { gridState.layoutInfo.visibleItemsInfo.toList() }
        onState { onDragInterrupted(true) }
        // Put the cards back for the next scroll position.
        compose.runOnIdle { items.sort() }
        compose.waitForIdle()
        return otherColumnTop.offset to after.firstOrNull { it.index == otherColumnTop.index }?.offset
    }

    @Test
    fun `dragging the first visible card never moves the other column at any scroll position`() {
        // Uneven cards, including swaps of cards with different heights, and scroll positions where
        // the first visible card is in either column.
        val heights = listOf(70, 40, 55, 80, 45, 90, 60, 50, 75, 65, 85, 40, 70, 55, 60, 80, 45, 95, 50, 70)
        setContent(columns = 2, count = heights.size, itemHeight = { heights[it] })

        var checked = 0
        for (scroll in 15..400 step 15) {
            for (down in listOf(true, false)) {
                val (before, after) = otherColumnTopAroundFirstCardDrag(scroll, down) ?: continue
                assertEquals("scroll $scroll, ${if (down) "down" else "up"}", before, after)
                checked++
            }
        }
        assertTrue("expected many reproduced cases, got $checked", checked >= 20)
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
