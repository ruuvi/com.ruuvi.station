package com.ruuvi.station.dashboard.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridLayoutInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

fun Modifier.dragGestureHandler(
    scope: CoroutineScope,
    itemStaggeredGridDragAndDropState: ItemStaggeredGridDragAndDropState,
    overscrollJob: MutableState<Job?>
): Modifier = this.pointerInput(Unit) {
    detectDragGesturesAfterLongPress(
        onDrag = { change, offset ->
            change.consume()
            itemStaggeredGridDragAndDropState.onDrag(offset)
            handleOverscrollJob(overscrollJob, scope, itemStaggeredGridDragAndDropState)
        },
        onDragStart = { offset ->
            itemStaggeredGridDragAndDropState.onDragStart(offset)
                      },
        onDragEnd = {
            itemStaggeredGridDragAndDropState.onDragInterrupted(false)
                    },
        onDragCancel = {
            itemStaggeredGridDragAndDropState.onDragInterrupted(true)
                       },
    )
}

private fun handleOverscrollJob(
    overscrollJob: MutableState<Job?>,
    scope: CoroutineScope,
    itemStaggeredGridDragAndDropState: ItemStaggeredGridDragAndDropState
) {
    if (overscrollJob.value?.isActive == true) return
    if (itemStaggeredGridDragAndDropState.checkForOverScroll() == 0f) return
    overscrollJob.value = scope.launch {
        itemStaggeredGridDragAndDropState.autoScroll()
    }
}

@Composable
fun rememberDragDropStaggeredGridState(
    lazyListState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onMove: (Int, Int) -> Unit,
    onDoneDragging: () -> Unit): ItemStaggeredGridDragAndDropState {
    val animationScope = rememberCoroutineScope()
    return remember {
        ItemStaggeredGridDragAndDropState(lazyListState, onMove, onDoneDragging, animationScope)
    }
}

/**
 * Places one dashboard card while cards are being reordered. Put it on the card's outermost
 * layout, after any zIndex, and give it the card's own key (the sensor id), not the grid key.
 *
 * The dragged card is drawn under the finger. Every other card slides from where it was drawn to
 * its new place whenever a move rearranges the grid, instead of jumping there.
 */
fun Modifier.dragAndDropPlacement(
    state: ItemStaggeredGridDragAndDropState,
    key: Any,
): Modifier {
    return this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            // Read the grid's placement and apply the offset in the same pass, so the frame that
            // moves a card already draws it at its old position.
            coordinates?.let { state.onItemPlaced(key, it.positionInParent().round()) }
            placeable.place(state.itemOffset(key))
        }
    }
}

class ItemStaggeredGridDragAndDropState(
    private val lazyListState: LazyStaggeredGridState,
    private val onMove: (Int, Int) -> Unit,
    private val onDoneDragging: () -> Unit,
    private val animationScope: CoroutineScope,
) {
    private var draggedDistance by mutableStateOf(Offset.Zero)
    private var dragStartPoint by mutableStateOf(Offset.Zero)
    private var initiallyDraggedElement by mutableStateOf<LazyStaggeredGridItemInfo?>(null)
    private var currentIndexOfDraggedItem by mutableIntStateOf(-1)
    private var overscrollJob by mutableStateOf<Job?>(null)

    var isDragInProgress by mutableStateOf(false)
        private set

    // Card placement, keyed by the card's own key. Plain fields: they are written and read during
    // placement of the same frame, so they must not trigger recomposition.
    private val placements = mutableMapOf<Any, ItemPlacement>()
    // Where each card was drawn just before the latest move, until the card is placed again.
    private val positionsBeforeMove = mutableMapOf<Any, IntOffset>()
    /**
     * The own key of the dragged card. The card is matched by this key, not by its slot: the list
     * a screen shows can update a frame after a move (a ViewModel flow, for example), and matching
     * by slot would then treat the wrong card as dragged for that frame, so it would flash.
     */
    var draggedKey by mutableStateOf<Any?>(null)
        private set
    private var draggedOrigin: IntOffset? = null

    // Retrieve the currently dragged element's info
    private val currentElement: LazyStaggeredGridItemInfo?
        get() = currentIndexOfDraggedItem.let { currentIndex ->
            lazyListState.getVisibleItemInfoFor(absoluteIndex = currentIndex)
        }

    // Calculate the initial offsets of the dragged element
    private val initialOffsets: IntOffset?
        get() = initiallyDraggedElement?.let { it.offset }

    // Calculate the displacement of the dragged element
    val elementDisplacement: IntOffset?
        get() = currentIndexOfDraggedItem
            .let { currentIndex -> lazyListState.getVisibleItemInfoFor(absoluteIndex = currentIndex) }
            ?.let { item ->
                (initiallyDraggedElement?.offset ?: IntOffset.Zero) + IntOffset(draggedDistance.x.toInt(), draggedDistance.y.toInt()) - item.offset
            }

    // Functions for handling drag gestures
    fun onDragStart(offset: Offset) {
        isDragInProgress = true
        dragStartPoint = offset
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset.y..(item.offset.y + item.size.height)
                    && offset.x.toInt() in item.offset.x..(item.offset.x + item.size.width)
            }
            ?.also {
                currentIndexOfDraggedItem = it.index
                initiallyDraggedElement = it
                // Items still have their own keys here: the slot keys only apply after this.
                draggedKey = it.key
                draggedOrigin = placements[it.key]?.let { placement ->
                    placement.position?.plus(placement.currentOffset())
                }
            }
    }

    fun onDrag(offset: Offset) {
        draggedDistance += offset
        val initial = initialOffsets ?: return
        val (startOffset, endOffset) = calculateOffsets(initial)

        val fingerOffset = dragStartPoint + draggedDistance

        val hoveredElement = currentElement
        if (hoveredElement != null) {
            val validItems = lazyListState.layoutInfo.visibleItemsInfo.filter { item ->
                hoveredElement.index != item.index && item.pointInside(fingerOffset)
            }

            val targetItem = validItems.firstOrNull { item ->
                val delta = startOffset - hoveredElement.offset

                val multiColumnCheck = if (hoveredElement.offset.x != item.offset.x) {
                    abs(item.offset.x - startOffset.x) < item.size.width * 0.2f
                } else {
                    true
                }

                multiColumnCheck && when {
                    delta.y > 0 -> endOffset.y > item.offset.y + item.size.height
                    else -> startOffset.y < item.offset.y
                }
            }

            if (targetItem != null) {
                // Items are keyed by slot while dragging (see itemKey), so this swap does not move
                // the scroll position in any column.
                rememberPositionsBeforeMove()
                onMove.invoke(currentIndexOfDraggedItem, targetItem.index)
                currentIndexOfDraggedItem = targetItem.index
            }
        }
    }

    /**
     * Key for the item at [index] whose own key is [key]. Use it as the grid's item key.
     *
     * A lazy grid keeps its scroll position anchored to the first visible item's key, so moving
     * that item would make the grid scroll to follow it. Restoring the position afterwards is not
     * enough for a staggered grid: each column keeps its own first item and offset, and the public
     * scroll API can only set one offset and rebuild the columns from one item, so the other
     * columns would still jump when cards have uneven heights. While a drag is in progress items
     * are therefore keyed by slot: a move only changes which content two slots show, and every
     * column stays exactly where it is. Real keys return when the drag ends; the last anchor key is
     * then not found, so the grid again keeps its position.
     */
    fun itemKey(index: Int, key: Any): Any = if (isDragInProgress) "$DRAG_SLOT_KEY_PREFIX$index" else key

    private companion object {
        // Lazy layout keys must be storable in a Bundle, so slot keys are strings; the prefix
        // cannot clash with sensor ids (MAC addresses).
        const val DRAG_SLOT_KEY_PREFIX = "dashboard-drag-slot:"
    }

    /**
     * A move swaps which cards the slots show, and cards of different heights make the staggered
     * grid reflow, so cards can land far from where they were, even in another column. Remember
     * where each card is drawn now, so it can slide from there once it is placed again.
     */
    private fun rememberPositionsBeforeMove() {
        positionsBeforeMove.clear()
        val currentLayoutInfo = lazyListState.layoutInfo
        placements.forEach { (key, placement) ->
            // Only cards placed in the latest layout are on screen; others have stale positions.
            if (placement.layoutInfo !== currentLayoutInfo) return@forEach
            val position = placement.position ?: return@forEach
            positionsBeforeMove[key] = position + placement.currentOffset()
        }
    }

    internal fun onItemPlaced(key: Any, position: IntOffset) {
        val placement = placements.getOrPut(key) { ItemPlacement() }
        placement.position = position
        placement.layoutInfo = lazyListState.layoutInfo
        val before = positionsBeforeMove.remove(key) ?: return
        // The dragged card follows the finger instead.
        if (key == draggedKey) return
        val delta = before - position
        if (delta != IntOffset.Zero) placement.slideFrom(delta, animationScope)
    }

    /** Whether the card whose own key is [key] is the one being dragged. */
    fun isDraggedItem(key: Any): Boolean = key == draggedKey

    internal fun itemOffset(key: Any): IntOffset {
        val placement = placements[key] ?: return IntOffset.Zero
        val position = placement.position ?: return IntOffset.Zero
        val origin = draggedOrigin
        if (key == draggedKey && origin != null) {
            // Computed here, during placement, from this frame's position, so the card stays under
            // the finger even in the frame where its slot moves.
            return origin + IntOffset(draggedDistance.x.roundToInt(), draggedDistance.y.roundToInt()) - position
        }
        return placement.currentOffset()
    }

    // Handle interrupted drag gesture
    fun onDragInterrupted(canceled: Boolean) {
        isDragInProgress = false
        positionsBeforeMove.clear()
        // Let the dropped card slide from under the finger into its slot instead of jumping there.
        val dropped = draggedKey
        val droppedPosition = dropped?.let { key -> placements[key]?.position?.plus(itemOffset(key)) }
        if (dropped != null && droppedPosition != null) positionsBeforeMove[dropped] = droppedPosition
        draggedKey = null
        draggedOrigin = null
        dragStartPoint = Offset.Unspecified
        draggedDistance = Offset.Zero
        currentIndexOfDraggedItem = -1
        initiallyDraggedElement = null
        overscrollJob?.cancel()
        if (!canceled) onDoneDragging.invoke()
    }

    private fun calculateOffsets(offset: IntOffset): Pair<IntOffset, IntOffset> {
        val startOffset = offset + IntOffset(draggedDistance.x.toInt(), draggedDistance.y.toInt())
        val currentElementSize = currentElement?.size ?: IntSize.Zero
        val endOffset = offset + IntOffset(draggedDistance.x.toInt(), draggedDistance.y.toInt()) + IntOffset(currentElementSize.width, currentElementSize.height)
        return startOffset to endOffset
    }

    fun LazyStaggeredGridItemInfo.pointInside(offset: Offset): Boolean {
        return offset.x >= this.offset.x && offset.x < (this.offset.x + this.size.width) &&
                offset.y >= this.offset.y && offset.y < (this.offset.y + this.size.height)
    }

    /**
     * Scrolls a little every frame while the dragged card is past the top or bottom edge, faster
     * the further past it is. Scrolling the whole overlap at once made the grid jump.
     */
    suspend fun autoScroll() {
        while (isDragInProgress) {
            val overscroll = checkForOverScroll()
            if (overscroll == 0f) return
            val step = (overscroll * AUTO_SCROLL_FRACTION_PER_FRAME)
                .coerceIn(-MAX_AUTO_SCROLL_PER_FRAME, MAX_AUTO_SCROLL_PER_FRAME)
            withFrameNanos { }
            if (lazyListState.scrollBy(step) == 0f) return
        }
    }

    fun checkForOverScroll(): Float {
        return initiallyDraggedElement?.let {
            val startOffset = it.offset.y + draggedDistance.y
            val endOffset = it.offset.y + it.size.height + draggedDistance.y

            return@let when {
                draggedDistance.y > 0 -> {
                    (endOffset - lazyListState.layoutInfo.viewportEndOffset).takeIf { diff -> diff > 0 }
                }
                draggedDistance.y < 0 -> {
                    (startOffset - lazyListState.layoutInfo.viewportStartOffset).takeIf { diff -> diff < 0 }
                }
                else -> null
            }
        } ?: 0f
    }

    fun getLazyListState(): LazyStaggeredGridState {
        return lazyListState
    }

    fun getCurrentIndexOfDraggedListItem(): Int {
        return currentIndexOfDraggedItem
    }
}

// The visible items of a staggered grid are not always a contiguous index range (a short card in
// one column can scroll out while the next index is still visible in another), so look the
// index up instead of computing its position in the list.
fun LazyStaggeredGridState.getVisibleItemInfoFor(absoluteIndex: Int): LazyStaggeredGridItemInfo? {
    return this.layoutInfo.visibleItemsInfo.firstOrNull { it.index == absoluteIndex }
}

private const val REORDER_SLIDE_MILLIS = 250
private const val AUTO_SCROLL_FRACTION_PER_FRAME = 0.15f
private const val MAX_AUTO_SCROLL_PER_FRAME = 40f

/** One card's slide to a new place after a move. */
private class ItemPlacement {
    /** Where the card was last placed by the grid, without any offset. */
    var position: IntOffset? = null
    /** The grid layout the card was last placed in; a newer one means it scrolled out. */
    var layoutInfo: LazyStaggeredGridLayoutInfo? = null
    private var slideDelta = IntOffset.Zero
    // Set until the animation has been reset, so the first frame already draws the old position.
    private var startPending = false
    private val progress = Animatable(1f)
    private var job: Job? = null

    fun currentOffset(): IntOffset {
        // Always read the progress, also while the start is pending: placement only re-runs for
        // state it read, so skipping the read would freeze the card until the slide ends.
        val progressValue = progress.value
        if (startPending) return slideDelta
        val remaining = 1f - progressValue
        return IntOffset((slideDelta.x * remaining).roundToInt(), (slideDelta.y * remaining).roundToInt())
    }

    fun slideFrom(delta: IntOffset, scope: CoroutineScope) {
        slideDelta = delta
        startPending = true
        job?.cancel()
        job = scope.launch {
            progress.snapTo(0f)
            startPending = false
            progress.animateTo(1f, tween(REORDER_SLIDE_MILLIS, easing = FastOutSlowInEasing))
        }
    }
}
