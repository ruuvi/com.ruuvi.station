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
import com.ruuvi.station.tagsettings.ui.offsetEnd
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
            Timber.d("dragGestureHandler - onDrag $offset")
            change.consume()
            itemStaggeredGridDragAndDropState.onDrag(offset)
            handleOverscrollJob(overscrollJob, scope, itemStaggeredGridDragAndDropState)
        },
        onDragStart = { offset ->
            Timber.d("dragGestureHandler - onDragStart $offset")
            itemStaggeredGridDragAndDropState.onDragStart(offset)
                      },
        onDragEnd = {
            Timber.d("dragGestureHandler - onDragEnd")
            itemStaggeredGridDragAndDropState.onDragInterrupted(false)
                    },
        onDragCancel = {
            Timber.d("dragGestureHandler - onDragCancel")
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
        dragStartPoint = offset
        for (item in lazyListState.layoutInfo.visibleItemsInfo) {
            Timber.d("dragGestureHandler onDragStart - visible item ${item.offset} ${item.size}")
        }
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset.y..(item.offset.y + item.size.height)
                    && offset.x.toInt() in item.offset.x..(item.offset.x + item.size.width)
            }
            ?.also {
                Timber.d("dragGestureHandler onDragStart - currentIndexOfDraggedItem ${it.index}")
                currentIndexOfDraggedItem = it.index
                initiallyDraggedElement = it
            }
    }

    fun onDrag(offset: Offset) {
        draggedDistance += offset
        Timber.d("dragGestureHandler onDrag draggedDistance $draggedDistance")
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

                when {
                    delta.y > 0 -> endOffset.y > item.offset.y + item.size.height
                    else -> startOffset.y < item.offset.y
                }
            }

            if (targetItem != null) {
                Timber.d("dragGestureHandler - onDrag targetItem $targetItem")
                currentIndexOfDraggedItem.let { current ->
                    onMove.invoke(current, targetItem.index)
                    currentIndexOfDraggedItem = targetItem.index
                }
            }
        }
    }

    // Handle interrupted drag gesture
    fun onDragInterrupted(canceled: Boolean) {
        Timber.d("dragGestureHandler - onDragInterrupted $canceled")
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

            Timber.d("checkForOverScroll startOffset = $startOffset endOffset = $endOffset viewPortStart = ${lazyListState.layoutInfo.viewportStartOffset} viewPortEnd = ${lazyListState.layoutInfo.viewportEndOffset}")
            return@let when {
                draggedDistance.y > 0 -> {
                    Timber.d("checkForOverScroll draggedDistance.y > 0 ${endOffset - lazyListState.layoutInfo.viewportEndOffset}")
                    (endOffset - lazyListState.layoutInfo.viewportEndOffset).takeIf { diff -> diff > 0 }
                }
                draggedDistance.y < 0 -> {
                    Timber.d("checkForOverScroll draggedDistance.y < 0 ${startOffset - lazyListState.layoutInfo.viewportStartOffset}")
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