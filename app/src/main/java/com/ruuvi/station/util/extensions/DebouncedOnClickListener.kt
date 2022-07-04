package com.ruuvi.station.util.extensions

import android.os.SystemClock
import android.view.View
import java.util.*

/**
 * A Debounced OnClickListener
 * Rejects clicks that are too close together in time.
 * This class is safe to use as an OnClickListener for multiple views, and will debounce each one separately.
 * The one and only constructor
 * @param minimumIntervalMilliseconds The minimum allowed time between clicks - any click sooner than this after a previous click will be rejected
 */
abstract class DebouncedOnClickListener(private val minimumIntervalMilliseconds: Long = 500) : View.OnClickListener {

    private val lastClickMap: MutableMap<View, Long> = WeakHashMap()

    /**
     * Implement this in your subclass instead of onClick
     * @param v The view that was clicked
     */
    abstract fun onDebouncedClick(v: View)

    override fun onClick(clickedView: View) {
        val previousClickTimestamp = lastClickMap[clickedView]
        val currentTimestamp = SystemClock.uptimeMillis()

        lastClickMap[clickedView] = currentTimestamp

        if (previousClickTimestamp == null
            || Math.abs(currentTimestamp - previousClickTimestamp) > minimumIntervalMilliseconds
        ) {
            onDebouncedClick(clickedView)
        }
    }
}

fun View.setDebouncedOnClickListener(clickListener: (View) -> Unit) {
    setOnClickListener(object : DebouncedOnClickListener() {
        override fun onDebouncedClick(v: View) {
            clickListener(v)
        }
    })
}