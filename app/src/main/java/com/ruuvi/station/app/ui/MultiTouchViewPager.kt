package com.ruuvi.station.app.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class MultiTouchViewPager(context: Context, attributeSet: AttributeSet) : ViewPager(context, attributeSet) {
    var isSwipeEnabled = true

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (isSwipeEnabled) {
            try {
                return super.onTouchEvent(ev)
            } catch (exception: IllegalArgumentException) {
                exception.printStackTrace()
            }
        }
        return false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (isSwipeEnabled) {
            try {
                return super.onInterceptTouchEvent(ev)
            } catch (exception: IllegalArgumentException) {
                exception.printStackTrace()
            }
        }
        return false
    }
}