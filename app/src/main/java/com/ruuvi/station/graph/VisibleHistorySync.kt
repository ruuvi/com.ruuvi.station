package com.ruuvi.station.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle

/** Place once at pager level; null means no full history view is visible. */
@Composable
fun VisibleHistorySync(sensorId: String?, observeHistory: suspend (String) -> Unit) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(sensorId, owner) {
        sensorId?.let {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { observeHistory(it) }
        }
    }
}
