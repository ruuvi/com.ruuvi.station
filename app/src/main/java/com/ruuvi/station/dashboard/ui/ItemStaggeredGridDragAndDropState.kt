package com.ruuvi.station.dashboard.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import timber.log.Timber

fun Modifier.dragGestureHandler(
    scope: CoroutineScope,
    itemStaggeredGridDragAndDropState: ItemStaggeredGridDragAndDropState,
    overscrollJob: MutableState<Job?>
): Modifier = this.pointerInput(Unit) {
    detectDragGesturesAfterLongPress(
        onDrag = { change, offset ->
            Timber.d("dragGestureHandler - onDrag")
            change.consume()
            itemStaggeredGridDragAndDropState.onDrag(offset)
            handleOverscrollJob(overscrollJob, scope, itemStaggeredGridDragAndDropState)
        },
        onDragStart = { offset -> itemStaggeredGridDragAndDropState.onDragStart(offset)
            Timber.d("dragGestureHandler - onDragStart $offset") },
        onDragEnd = { itemStaggeredGridDragAndDropState.onDragInterrupted(false)
            Timber.d("dragGestureHandler - onDragEnd") },
        onDragCancel = { itemStaggeredGridDragAndDropState.onDragInterrupted(true)
            Timber.d("dragGestureHandler - onDragCancel") }
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
    return remember { ItemStaggeredGridDragAndDropState(lazyListState, onMove, onDoneDragging) }
}
class ItemStaggeredGridDragAndDropState(
    private val lazyListState: LazyStaggeredGridState,
    private val onMove: (Int, Int) -> Unit,
    private val onDoneDragging: () -> Unit
) {
    private var draggedDistance by mutableStateOf(Offset.Zero)
    private var initialOffset by mutableStateOf(Offset.Zero)
    private var initiallyDraggedElement by mutableStateOf<LazyStaggeredGridItemInfo?>(null)
    private var currentIndexOfDraggedItem by mutableIntStateOf(-1)
    private var overscrollJob by mutableStateOf<Job?>(null)

    var isDragInProgress: Boolean = false

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
        initialOffset = offset
        for (item in lazyListState.layoutInfo.visibleItemsInfo) {
            Timber.d("dragGestureHandler - visible item ${item.offset} ${item.size}")
        }
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset.y..(item.offset.y + item.size.height)
                    && offset.x.toInt() in item.offset.x..(item.offset.x + item.size.width)
            }
            ?.also {
                Timber.d("dragGestureHandler - currentIndexOfDraggedItem ${it.index}")
                currentIndexOfDraggedItem = it.index
                initiallyDraggedElement = it
            }
    }

    // Handle interrupted drag gesture
    fun onDragInterrupted(canceled: Boolean) {
        isDragInProgress = false
        initialOffset = Offset.Unspecified
        draggedDistance = Offset.Zero
        currentIndexOfDraggedItem = -1
        initiallyDraggedElement = null
        overscrollJob?.cancel()
        if (!canceled) onDoneDragging.invoke()
    }

    // Helper function to calculate start and end offsets
    // Calculate the start and end offsets of the dragged element
//    private fun calculateOffsets(offset: Float): Pair<Float, Float> {
//        val startOffset = offset + draggedDistance
//        val currentElementSize = currentElement?.size ?: IntSize.Zero
//        val endOffset = offset + draggedDistance + currentElementSize
//        return startOffset to endOffset
//    }

    private fun calculateOffsets(offset: IntOffset): Pair<IntOffset, IntOffset> {
        val startOffset = offset + IntOffset(draggedDistance.x.toInt(), draggedDistance.y.toInt())
        val currentElementSize = currentElement?.size ?: IntSize.Zero
        val endOffset = offset + IntOffset(draggedDistance.x.toInt(), draggedDistance.y.toInt()) + IntOffset(currentElementSize.width, currentElementSize.height)
        return startOffset to endOffset
    }

    fun onDrag(offset: Offset) {

        draggedDistance += offset
        Timber.d("onDrag offset $offset draggedDistance $draggedDistance")
        val initial = initialOffsets ?: return
        val (startOffset, endOffset) = calculateOffsets(initial)

        val fingerOffset = initialOffset + draggedDistance

        val hoveredElement = currentElement
        if (hoveredElement != null) {
            val validItems = lazyListState.layoutInfo.visibleItemsInfo.filter { item ->
                hoveredElement.index != item.index && item.pointInside(fingerOffset)
            }

            val targetItem = validItems.sortedBy { it.offset.y }.firstOrNull()

            if (targetItem != null) {
                currentIndexOfDraggedItem.let { current ->
                    onMove.invoke(current, targetItem.index)
                    currentIndexOfDraggedItem = targetItem.index
                }
            }
        }
    }

    fun LazyStaggeredGridItemInfo.pointInside(offset: Offset): Boolean {
        return offset.x >= this.offset.x && offset.x < (this.offset.x + this.size.width) &&
                offset.y >= this.offset.y && offset.y < (this.offset.y + this.size.height)
    }

    fun checkForOverScroll(): Float {
        val draggedElement = initiallyDraggedElement
        if (draggedElement != null) {
            val (startOffset, endOffset) = calculateOffsets(draggedElement.offset)
            Timber.d("lazyListState ${lazyListState.layoutInfo.viewportEndOffset} ${lazyListState.layoutInfo.viewportStartOffset}")
            Timber.d("lazyListState startOffset, endOffset ${startOffset} ${endOffset}")

            val diffToEnd = endOffset.y - lazyListState.layoutInfo.viewportEndOffset
            val diffToStart = startOffset.y - lazyListState.layoutInfo.viewportStartOffset
            return when {
                draggedDistance.y > 0 && diffToEnd > 0 -> diffToEnd.toFloat()
                draggedDistance.y < 0 && diffToStart < 0 -> diffToStart.toFloat()
                else -> 0f
            }
        }
        return 0f
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