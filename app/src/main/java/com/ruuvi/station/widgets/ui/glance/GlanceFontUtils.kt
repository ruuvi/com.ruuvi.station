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

object GlanceFontUtils {
    private val typefaceCache = LruCache<Int, Typeface?>(6)

    fun createFontBitmap(
        context: Context,
        text: String,
        fontSize: TextUnit,
        color: Color,
        @FontRes fontResId: Int,
        maxWidth: Int? = null
    ): Bitmap {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = fontSize.value * context.resources.displayMetrics.scaledDensity
        paint.color = color.toArgb()
        
        paint.typeface = getCachedTypeface(context, fontResId)

        val horizontalPadding = 0f
        val verticalPadding = 0f

        var textToDraw = text
        if (maxWidth != null && maxWidth > 0) {
            textToDraw = TextUtils.ellipsize(text, paint, maxWidth.toFloat(), TextUtils.TruncateAt.END).toString()
        }

        val metrics = paint.fontMetrics
        val width = ceil(paint.measureText(textToDraw) + (horizontalPadding * 2)).toInt()
        val height = ceil((metrics.descent - metrics.ascent) + (verticalPadding * 2)).toInt()

        if (width <= 0 || height <= 0) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        val baseline = verticalPadding - metrics.ascent
        canvas.drawText(textToDraw, horizontalPadding, baseline, paint)
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

@Composable
fun CustomFontText(
    text: String,
    fontSize: TextUnit,
    colorProvider: ColorProvider,
    @FontRes fontResId: Int,
    modifier: GlanceModifier = GlanceModifier,
    maxWidth: Dp? = null
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
        maxWidth = maxWidthPx
    )
    Box(modifier = modifier) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = text
        )
    }
}
