package com.ruuvi.station.util.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.WindowInsets
import android.widget.ScrollView
import androidx.core.view.WindowInsetsCompat

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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bottomFade.refreshFadeLength(this)
    }

    override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
        bottomFade.refreshFadeLength(this, WindowInsetsCompat.toWindowInsetsCompat(insets, this))
        return super.dispatchApplyWindowInsets(insets)
    }

    override fun dispatchDraw(canvas: Canvas) {
        bottomFade.draw(canvas, this) { super.dispatchDraw(canvas) }
    }
}
