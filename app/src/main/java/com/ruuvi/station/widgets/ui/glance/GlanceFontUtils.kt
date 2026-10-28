package com.ruuvi.station.widgets.ui.glance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.util.LruCache
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.TextView
import androidx.annotation.FontRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.TypedValueCompat
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.R as GlanceR
import androidx.glance.layout.Box
import androidx.glance.unit.ColorProvider
import kotlin.math.ceil

private const val END_ELLIPSIS = "\u2026"
private const val BITMAP_ROW_ALIGNMENT_BYTES = 4
private const val NUMERIC_CAP_HEIGHT_TEXT = "0"

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
        val bitmapFormat = fontBitmapFormat(embeddedColor)

        val metrics = paint.fontMetrics
        val height = ceil(metrics.descent - metrics.ascent).toInt().coerceAtLeast(1)
        val bitmapMaxWidth = calculateBitmapMaxWidth(
            requestedWidth = maxWidth,
            bitmapHeight = height,
            bytesPerPixel = bitmapFormat.bytesPerPixel,
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

        // Generated previews use a self-contained colored bitmap because widget
        // hosts can drop a separate tint while persisting the preview. Placed
        // widgets use a neutral alpha mask so their day/night tint stays dynamic.
        val image = Bitmap.createBitmap(width, height, bitmapFormat.config).apply {
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

    internal fun measureCustomFontWidth(
        context: Context,
        text: String,
        fontSize: TextUnit,
        @FontRes fontResId: Int
    ): Dp {
        val widthPx = ceil(createTextPaint(context, fontSize, fontResId).measureText(text))
        return (widthPx.coerceAtLeast(1f) / context.resources.displayMetrics.density).dp
    }

    internal fun measureSystemFontHeight(
        context: Context,
        fontSize: TextUnit,
        bold: Boolean
    ): Dp = measureSystemFontMetrics(context, NUMERIC_CAP_HEIGHT_TEXT, fontSize, bold).height

    internal fun measureSystemFontBaselineOffset(
        context: Context,
        fontSize: TextUnit,
        bold: Boolean
    ): Dp = measureSystemFontMetrics(context, NUMERIC_CAP_HEIGHT_TEXT, fontSize, bold).baseline

    internal fun measureCustomFontMetrics(
        context: Context,
        fontSize: TextUnit,
        @FontRes fontResId: Int
    ): WidgetTextMetrics {
        val paint = createTextPaint(context, fontSize, fontResId)
        val metrics = paint.fontMetrics
        return measureTextMetrics(
            context = context,
            paint = paint,
            text = NUMERIC_CAP_HEIGHT_TEXT,
            baseline = -metrics.ascent,
            height = ceil(metrics.descent - metrics.ascent)
        )
    }

    internal fun measureSystemFontMetrics(
        context: Context,
        text: String,
        fontSize: TextUnit,
        bold: Boolean
    ): WidgetTextMetrics {
        val themedContext = ContextThemeWrapper(context, GlanceR.style.Glance_AppWidget_Theme)
        val appearance = if (bold) {
            TextAppearanceSpan(themedContext, GlanceR.style.Glance_AppWidget_TextAppearance_Bold)
        } else {
            null
        }
        val styledText = if (appearance == null) {
            text
        } else {
            SpannableString(text).apply {
                setSpan(appearance, 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
        // Device-default fonts and TextView line metrics can differ from a standalone Paint.
        val nativeText = TextView(themedContext).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize.value)
            maxLines = 1
            textDirection = View.TEXT_DIRECTION_LOCALE
            this.text = styledText
            val unconstrained = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            measure(unconstrained, unconstrained)
        }
        val paint = TextPaint(nativeText.paint).apply {
            appearance?.updateMeasureState(this)
        }
        return measureTextMetrics(
            context = context,
            paint = paint,
            text = text,
            baseline = nativeText.baseline.toFloat(),
            height = nativeText.measuredHeight.toFloat(),
            width = nativeText.measuredWidth.toFloat()
        )
    }

    internal fun fitCustomFontSizeToBaselineHeight(
        context: Context,
        fontSize: TextUnit,
        @FontRes fontResId: Int,
        minimumHeight: Dp
    ): TextUnit {
        val displayMetrics = context.resources.displayMetrics
        val minimumHeightPx = ceil(minimumHeight.value * displayMetrics.density).coerceAtLeast(1f)
        val bounds = Rect()
        var lowerSizeSp = 0f
        var upperSizeSp = maxOf(
            fontSize.value,
            TypedValueCompat.pxToSp(minimumHeightPx * 2, displayMetrics)
        )

        fun capHeight(sizeSp: Float): Int {
            createTextPaint(context, sizeSp.sp, fontResId).getTextBounds(
                NUMERIC_CAP_HEIGHT_TEXT, 0, NUMERIC_CAP_HEIGHT_TEXT.length, bounds
            )
            return -bounds.top
        }

        while (capHeight(upperSizeSp) < minimumHeightPx) {
            upperSizeSp *= 2
            check(upperSizeSp.isFinite()) { "Custom numeric font must have a positive cap height" }
        }

        // Measure the actual SP candidates so pixel/SP rounding cannot undo the fit.
        repeat(12) {
            val candidate = (lowerSizeSp + upperSizeSp) / 2
            if (capHeight(candidate) < minimumHeightPx) {
                lowerSizeSp = candidate
            } else {
                upperSizeSp = candidate
            }
        }

        return upperSizeSp.sp
    }

    private fun measureTextMetrics(
        context: Context,
        paint: TextPaint,
        text: String,
        baseline: Float,
        height: Float,
        width: Float = ceil(paint.measureText(text))
    ): WidgetTextMetrics {
        val density = context.resources.displayMetrics.density
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        return WidgetTextMetrics(
            height = (height.coerceAtLeast(1f) / density).dp,
            baseline = (baseline / density).dp,
            glyphTop = ((baseline + bounds.top) / density).dp,
            glyphBottom = ((baseline + bounds.bottom) / density).dp,
            width = (width / density).dp
        )
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

internal data class WidgetTextMetrics(
    val height: Dp,
    val baseline: Dp,
    val glyphTop: Dp,
    val glyphBottom: Dp,
    val width: Dp = 0.dp
)

internal data class FontBitmapFormat(
    val config: Bitmap.Config,
    val bytesPerPixel: Int,
)

internal fun fontBitmapFormat(embeddedColor: Int?): FontBitmapFormat =
    if (embeddedColor == null) {
        FontBitmapFormat(
            config = Bitmap.Config.ALPHA_8,
            bytesPerPixel = 1,
        )
    } else {
        FontBitmapFormat(
            config = Bitmap.Config.ARGB_8888,
            bytesPerPixel = 4,
        )
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
    val maxWidthPx = customFontBitmapMaxWidthPx(maxWidth, density)

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

internal fun customFontBitmapMaxWidthPx(maxWidth: Dp, density: Float): Int =
    ceil(maxWidth.value * density).toInt().coerceAtLeast(1)
