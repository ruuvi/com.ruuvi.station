package com.ruuvi.station.widgets.ui.glance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.util.LruCache
import androidx.annotation.FontRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.core.content.res.ResourcesCompat
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.Box
import androidx.glance.unit.ColorProvider
import kotlin.math.ceil

data class FontBitmapSize(
    val width: Int,
    val height: Int,
    val baseline: Int
)

object GlanceFontUtils {
    private val typefaceCache = LruCache<Int, Typeface?>(6)

    fun measureFontBitmap(
        context: Context,
        text: String,
        fontSize: TextUnit,
        @FontRes fontResId: Int,
        maxWidth: Int? = null,
        maxFontScale: Float = Float.POSITIVE_INFINITY
    ): FontBitmapSize {
        if (text.isEmpty()) {
            return FontBitmapSize(0, 0, 0)
        }

        val paint = createTextPaint(context, fontSize, Color.Black, fontResId, maxFontScale)
        val textToMeasure = ellipsizeText(text, paint, maxWidth)
        return measureTextBitmap(paint, textToMeasure)
    }

    fun createFontBitmap(
        context: Context,
        text: String,
        fontSize: TextUnit,
        color: Color,
        @FontRes fontResId: Int,
        maxWidth: Int? = null,
        maxFontScale: Float = Float.POSITIVE_INFINITY
    ): Bitmap {
        val paint = createTextPaint(context, fontSize, color, fontResId, maxFontScale)
        val textToDraw = ellipsizeText(text, paint, maxWidth)
        val size = measureTextBitmap(paint, textToDraw)

        if (size.width <= 0 || size.height <= 0) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val image = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        val metrics = paint.fontMetrics
        canvas.drawText(textToDraw, 0f, -metrics.ascent, paint)
        return image
    }

    private fun createTextPaint(
        context: Context,
        fontSize: TextUnit,
        color: Color,
        @FontRes fontResId: Int,
        maxFontScale: Float
    ): TextPaint {
        val appliedFontScale = context.resources.configuration.fontScale.coerceAtMost(maxFontScale)
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            // Match Compose's limitScaleTo behavior: render with system font scale, but let
            // callers cap it for compact widget layouts that mirror dashboard text.
            textSize = fontSize.value * context.resources.displayMetrics.density * appliedFontScale
            this.color = color.toArgb()
            typeface = getCachedTypeface(context, fontResId)
        }
    }

    private fun ellipsizeText(text: String, paint: TextPaint, maxWidth: Int?): String {
        return if (maxWidth != null && maxWidth > 0) {
            TextUtils.ellipsize(text, paint, maxWidth.toFloat(), TextUtils.TruncateAt.END).toString()
        } else {
            text
        }
    }

    private fun measureTextBitmap(paint: TextPaint, text: String): FontBitmapSize {
        val metrics = paint.fontMetrics
        val width = ceil(paint.measureText(text)).toInt()
        val height = ceil(metrics.descent - metrics.ascent).toInt()
        val baseline = ceil(-metrics.ascent).toInt()
        return FontBitmapSize(width, height, baseline)
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

@Composable
fun CustomFontText(
    text: String,
    fontSize: TextUnit,
    colorProvider: ColorProvider,
    @FontRes fontResId: Int,
    modifier: GlanceModifier = GlanceModifier,
    maxWidth: Dp? = null,
    maxFontScale: Float = Float.POSITIVE_INFINITY
) {
    if (text.isEmpty()) return
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val maxWidthPx = maxWidth?.let { (it.value * density).toInt() }

    val bitmap = GlanceFontUtils.createFontBitmap(
        context = context,
        text = text,
        fontSize = fontSize,
        color = colorProvider.getColor(context),
        fontResId = fontResId,
        maxWidth = maxWidthPx,
        maxFontScale = maxFontScale
    )
    Box(modifier = modifier) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = text
        )
    }
}
