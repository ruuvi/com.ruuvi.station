package com.ruuvi.station.util.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/** Shared View renderer; masks children without changing the background or system bars. */
internal class BottomFadingEdgeRenderer(context: Context, attrs: AttributeSet?) {
    // Read explicit XML/style overrides, avoiding the platform widgets' different fade defaults.
    private val length = context.obtainStyledAttributes(
        attrs,
        intArrayOf(android.R.attr.fadingEdgeLength),
    ).let { attributes ->
        try {
            attributes.getDimension(
                0,
                BottomFadingEdgeDefaults.LENGTH_DP * context.resources.displayMetrics.density,
            )
        } finally {
            attributes.recycle()
        }
    }
    private val paint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }
    private var cachedStart = Float.NaN
    private var cachedEnd = Float.NaN

    fun draw(canvas: Canvas, view: View, drawContent: () -> Unit) {
        val end = view.height.toFloat()
        val start = (end - length).coerceAtLeast(0f)
        if (!view.canScrollVertically(1) || end <= start) {
            drawContent()
            return
        }
        if (start != cachedStart || end != cachedEnd) {
            paint.shader = LinearGradient(
                0f, start, 0f, end,
                Color.TRANSPARENT, Color.BLACK,
                Shader.TileMode.CLAMP,
            )
            cachedStart = start
            cachedEnd = end
        }
        val layer = canvas.saveLayer(
            view.scrollX.toFloat(),
            view.scrollY.toFloat(),
            (view.scrollX + view.width).toFloat(),
            (view.scrollY + view.height).toFloat(),
            null,
        )
        try {
            drawContent()
            // Children use content coordinates; anchor the mask to the visible viewport.
            canvas.translate(view.scrollX.toFloat(), view.scrollY.toFloat())
            canvas.drawRect(0f, start, view.width.toFloat(), end, paint)
        } finally {
            canvas.restoreToCount(layer)
        }
    }
}
