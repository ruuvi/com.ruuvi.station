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
import android.util.TypedValue
import androidx.annotation.FontRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.Box
import androidx.glance.unit.ColorProvider
import kotlin.math.ceil

private const val END_ELLIPSIS = "\u2026"
private const val BITMAP_ROW_ALIGNMENT_BYTES = 4

object GlanceFontUtils {
    internal const val MAX_BITMAP_BYTES = 96 * 1024
    private val typefaceCache = LruCache<Int, Typeface?>(6)

    fun createFontBitmap(
        context: Context,
        text: String,
        fontSize: TextUnit,
        @FontRes fontResId: Int,
        maxWidth: Int,
        embeddedColor: Int? = null
    ): Bitmap {
        require(maxWidth > 0) { "Custom font text requires a positive maximum width" }

        val paint = createTextPaint(context, fontSize, fontResId).apply {
            embeddedColor?.let { color = it }
        }
        val bitmapConfig = if (embeddedColor == null) {
            Bitmap.Config.ALPHA_8
        } else {
            Bitmap.Config.ARGB_8888
        }
        val bytesPerPixel = if (bitmapConfig == Bitmap.Config.ALPHA_8) 1 else 4

        val metrics = paint.fontMetrics
        val height = ceil(metrics.descent - metrics.ascent).toInt().coerceAtLeast(1)
        val bitmapMaxWidth = calculateBitmapMaxWidth(
            requestedWidth = maxWidth,
            bitmapHeight = height,
            bytesPerPixel = bytesPerPixel
        )
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

        // A supplied color produces a self-contained bitmap. This is required for
        // Android versions that can drop a separate bitmap tint in widget content
        // or while persisting a generated preview.
        val image = Bitmap.createBitmap(width, height, bitmapConfig).apply {
            density = context.resources.displayMetrics.densityDpi
        }
        val canvas = Canvas(image)
        canvas.scale(horizontalScale, 1f)
        canvas.drawText(textToDraw, 0f, -metrics.ascent, paint)
        return image
    }

    internal fun measureCustomFontHeight(
        context: Context,
        fontSize: TextUnit,
        @FontRes fontResId: Int
    ): Dp {
        val paint = createTextPaint(context, fontSize, fontResId)
        val metrics = paint.fontMetrics
        val heightPx = ceil(metrics.descent - metrics.ascent)
        return (heightPx / context.resources.displayMetrics.density).dp
    }

    internal fun measureSystemFontHeight(
        context: Context,
        fontSize: TextUnit,
        bold: Boolean
    ): Dp {
        val paint = createSystemTextPaint(context, fontSize, bold)
        val metrics = paint.fontMetricsInt
        val heightPx = (metrics.bottom - metrics.top).coerceAtLeast(1)
        return (heightPx / context.resources.displayMetrics.density).dp
    }

    internal fun measureSystemFontBaselineOffset(
        context: Context,
        fontSize: TextUnit,
        bold: Boolean
    ): Dp {
        val metrics = createSystemTextPaint(context, fontSize, bold).fontMetricsInt
        return (-metrics.top / context.resources.displayMetrics.density).dp
    }

    private fun createSystemTextPaint(
        context: Context,
        fontSize: TextUnit,
        bold: Boolean
    ) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            fontSize.value,
            context.resources.displayMetrics
        )
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun createTextPaint(
        context: Context,
        fontSize: TextUnit,
        @FontRes fontResId: Int
    ) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            fontSize.value,
            context.resources.displayMetrics
        )
        color = Color.WHITE
        typeface = getCachedTypeface(context, fontResId)
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

internal fun calculateAllocationSafeBitmapWidth(
    bitmapHeight: Int,
    bytesPerPixel: Int = 1
): Int {
    val safeHeight = bitmapHeight.coerceAtLeast(1)
    val safeBytesPerPixel = bytesPerPixel.coerceAtLeast(1)
    val maxPixelsPerRow = (
        GlanceFontUtils.MAX_BITMAP_BYTES / safeHeight / safeBytesPerPixel
    ).coerceAtLeast(1)
    return if (safeBytesPerPixel >= BITMAP_ROW_ALIGNMENT_BYTES) {
        maxPixelsPerRow
    } else if (maxPixelsPerRow < BITMAP_ROW_ALIGNMENT_BYTES) {
        1
    } else {
        maxPixelsPerRow - (maxPixelsPerRow % BITMAP_ROW_ALIGNMENT_BYTES)
    }
}

internal fun calculateBitmapMaxWidth(
    requestedWidth: Int,
    bitmapHeight: Int,
    bytesPerPixel: Int = 1
): Int = minOf(
    requestedWidth.coerceAtLeast(1),
    calculateAllocationSafeBitmapWidth(bitmapHeight, bytesPerPixel)
)

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
    maxWidth: Dp,
    embeddedColor: Int? = null
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
        maxWidth = maxWidthPx,
        embeddedColor = embeddedColor
    )
    Box(modifier = modifier) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = text,
            colorFilter = if (embeddedColor == null) {
                ColorFilter.tint(colorProvider)
            } else {
                null
            }
        )
    }
}
