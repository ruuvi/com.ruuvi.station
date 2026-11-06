package com.ruuvi.station.util.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.ScrollView

/**
 * Bottom-only fade for XML scrolling content, including behind transparent navigation.
 * Defaults to 64sp plus navigation inset; android:fadingEdgeLength can override it. No fade at the end.
 */
class BottomFadingEdgeScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.scrollViewStyle,
) : ScrollView(context, attrs, defStyleAttr) {
    private val bottomFade = BottomFadingEdgeRenderer(context, attrs)

    init {
        isVerticalFadingEdgeEnabled = false
    }

    override fun dispatchDraw(canvas: Canvas) {
        bottomFade.draw(canvas, this) { super.dispatchDraw(canvas) }
    }
}
