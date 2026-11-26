package com.ruuvi.station.dashboard.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    val overscrollOffset = itemStaggeredGridDragAndDropState.checkForOverScroll()
    if (overscrollOffset != 0f) {
        overscrollJob.value = scope.launch {
            itemStaggeredGridDragAndDropState.getLazyListState().scrollBy(overscrollOffset)
        }
    } else {
        overscrollJob.value?.cancel()
    }
}

@Composable
fun rememberDragDropStaggeredGridState(
    lazyListState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onMove: (Int, Int) -> Unit,
    onDoneDragging: () -> Unit): ItemStaggeredGridDragAndDropState {
    return remember {
        ItemStaggeredGridDragAndDropState(lazyListState, onMove, onDoneDragging)
    }
}
class ItemStaggeredGridDragAndDropState(
    private val lazyListState: LazyStaggeredGridState,
    private val onMove: (Int, Int) -> Unit,
    private val onDoneDragging: () -> Unit
) {
    private var draggedDistance by mutableStateOf(Offset.Zero)
    private var dragStartPoint by mutableStateOf(Offset.Zero)
    private var initiallyDraggedElement by mutableStateOf<LazyStaggeredGridItemInfo?>(null)
    private var currentIndexOfDraggedItem by mutableIntStateOf(-1)
    private var overscrollJob by mutableStateOf<Job?>(null)

    var isDragInProgress by mutableStateOf(false)
        private set

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

    // Handle interrupted drag gesture
    fun onDragInterrupted(canceled: Boolean) {
        isDragInProgress = false
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

fun LazyStaggeredGridState.getVisibleItemInfoFor(absoluteIndex: Int): LazyStaggeredGridItemInfo? {
    return this.layoutInfo.visibleItemsInfo.getOrNull(
        absoluteIndex - this.layoutInfo.visibleItemsInfo.first().index
    )
}
