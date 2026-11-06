package com.ruuvi.station.util.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.ListView

/**
 * Bottom-only fade for XML lists, including behind transparent navigation.
 * Defaults to 64sp plus navigation inset; android:fadingEdgeLength can override it. No fade at the end.
 */
class BottomFadingEdgeListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.listViewStyle,
) : ListView(context, attrs, defStyleAttr) {
    private val bottomFade = BottomFadingEdgeRenderer(context, attrs)

    init {
        isVerticalFadingEdgeEnabled = false
    }

    override fun dispatchDraw(canvas: Canvas) {
        bottomFade.draw(canvas, this) { super.dispatchDraw(canvas) }
    }
}
