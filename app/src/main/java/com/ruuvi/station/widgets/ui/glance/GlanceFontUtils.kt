package com.ruuvi.station.widgets.ui.glance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.util.LruCache
import androidx.annotation.FontRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.core.content.res.ResourcesCompat
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.Box
import androidx.glance.unit.ColorProvider
import kotlin.math.ceil
import kotlin.math.min

private const val END_ELLIPSIS = "\u2026"

object GlanceFontUtils {
    internal const val MAX_BITMAP_BYTES = 96 * 1024
    private const val BYTES_PER_PIXEL = 4
    private val typefaceCache = LruCache<Int, Typeface?>(6)

    fun createFontBitmap(
        context: Context,
        text: String,
        fontSize: TextUnit,
        @FontRes fontResId: Int,
        maxWidth: Int
    ): Bitmap {
        require(maxWidth > 0) { "Custom font text requires a positive maximum width" }

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = fontSize.value * context.resources.displayMetrics.scaledDensity
        paint.color = Color.WHITE
        paint.typeface = getCachedTypeface(context, fontResId)

        val metrics = paint.fontMetrics
        val height = ceil(metrics.descent - metrics.ascent).toInt().coerceAtLeast(1)
        val allocationSafeWidth = (MAX_BITMAP_BYTES / BYTES_PER_PIXEL / height).coerceAtLeast(1)
        val bitmapMaxWidth = min(maxWidth, allocationSafeWidth)
        val isTruncated = paint.measureText(text) > bitmapMaxWidth
        val platformEllipsizedText = TextUtils.ellipsize(
            text,
            paint,
            bitmapMaxWidth.toFloat(),
            TextUtils.TruncateAt.END
        ).toString()
        val textToDraw = ensureTrailingEllipsis(
            text = platformEllipsizedText,
            isTruncated = isTruncated
        )
        val measuredWidth = paint.measureText(textToDraw).coerceAtLeast(1f)
        val horizontalScale = (bitmapMaxWidth / measuredWidth).coerceAtMost(1f)
        val width = ceil(measuredWidth * horizontalScale).toInt().coerceIn(1, bitmapMaxWidth)

        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            density = context.resources.displayMetrics.densityDpi
        }
        val canvas = Canvas(image)
        canvas.scale(horizontalScale, 1f)
        canvas.drawText(textToDraw, 0f, -metrics.ascent, paint)
        return image
    }

    private fun getCachedTypeface(context: Context, fontResId: Int): Typeface? {
        var typeface = typefaceCache.get(fontResId)
        if (typeface == null) {
            typeface = ResourcesCompat.getFont(context, fontResId)
            if (typeface != null) {
                typefaceCache.put(fontResId, typeface)
            }
        }
        return typeface
    }
}

internal fun ensureTrailingEllipsis(
    text: String,
    isTruncated: Boolean
): String = when {
    !isTruncated -> text
    text.endsWith(END_ELLIPSIS) -> text
    else -> "$text$END_ELLIPSIS"
}

@Composable
fun CustomFontText(
    text: String,
    fontSize: TextUnit,
    colorProvider: ColorProvider,
    @FontRes fontResId: Int,
    modifier: GlanceModifier = GlanceModifier,
    maxWidth: Dp
) {
    if (text.isEmpty()) return
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val maxWidthPx = (maxWidth.value * density).toInt().coerceAtLeast(1)

    val bitmap = GlanceFontUtils.createFontBitmap(
        context = context,
        text = text,
        fontSize = fontSize,
        fontResId = fontResId,
        maxWidth = maxWidthPx
    )
    Box(modifier = modifier) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = text,
            colorFilter = ColorFilter.tint(colorProvider)
        )
    }
}
