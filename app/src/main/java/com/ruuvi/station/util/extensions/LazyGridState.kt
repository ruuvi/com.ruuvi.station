package com.ruuvi.station.util.extensions

import androidx.compose.foundation.lazy.grid.LazyGridState

suspend fun LazyGridState.centerViewportOnItem(index: Int) {
    val itemHeight = this.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: 0
    this.animateScrollToItem(index, - (this.layoutInfo.viewportEndOffset / 2 - itemHeight / 2))
}