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
import android.util.TypedValue
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** Shared View renderer; masks children without changing the background or system bars. */
internal class BottomFadingEdgeRenderer(context: Context, attrs: AttributeSet?) {
    // Read explicit XML/style overrides, avoiding the platform widgets' different fade defaults.
    private val length = context.obtainStyledAttributes(
        attrs,
        intArrayOf(android.R.attr.fadingEdgeLength),
    ).let { attributes ->
        try {
            if (attributes.hasValue(0)) attributes.getDimension(0, 0f) else null
        } finally {
            attributes.recycle()
        }
    }
    private val paint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }
    private var cachedStart = Float.NaN
    private var cachedEnd = Float.NaN
    private var fadeLength = length ?: TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        BottomFadingEdgeDefaults.LENGTH_SP.toFloat(),
        context.resources.displayMetrics,
    )

    fun refreshFadeLength(
        view: View,
        insets: WindowInsetsCompat? = ViewCompat.getRootWindowInsets(view),
    ) {
        if (length != null) return
        val navigationBarInset = insets
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        fadeLength = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            BottomFadingEdgeDefaults.LENGTH_SP.toFloat(),
            view.resources.displayMetrics,
        ) + navigationBarInset
        view.invalidate()
    }

    fun draw(canvas: Canvas, view: View, drawContent: () -> Unit) {
        if (!view.canScrollVertically(1)) {
            drawContent()
            return
        }
        val end = view.height.toFloat()
        val start = (end - fadeLength).coerceAtLeast(0f)
        if (end <= start) {
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
