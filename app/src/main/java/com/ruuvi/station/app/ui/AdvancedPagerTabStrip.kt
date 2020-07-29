package com.ruuvi.station.app.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.PagerTabStrip

class AdvancedPagerTabStrip(context: Context, attributeSet: AttributeSet) : PagerTabStrip(context, attributeSet) {
    var isTabSwitchEnabled = true

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return if (isTabSwitchEnabled) super.onInterceptTouchEvent(ev) else true
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return if (isTabSwitchEnabled) super.onTouchEvent(ev) else true
    }
}