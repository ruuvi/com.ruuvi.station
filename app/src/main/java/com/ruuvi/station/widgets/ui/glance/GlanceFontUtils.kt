package com.ruuvi.station.widgets.ui.glance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.core.content.res.ResourcesCompat
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.Box
import androidx.glance.unit.ColorProvider
import com.ruuvi.station.R
import kotlin.math.ceil

object GlanceFontUtils {
    fun createFontBitmap(
        context: Context,
        text: String,
        fontSize: TextUnit,
        color: Color,
        fontFamily: FontFamily
    ): Bitmap {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = fontSize.value * context.resources.displayMetrics.scaledDensity
        paint.color = color.toArgb()
        
        val fontResId = when (fontFamily) {
            com.ruuvi.station.app.ui.theme.ruuviStationFonts.mulishBold -> R.font.mulish_bold
            com.ruuvi.station.app.ui.theme.ruuviStationFonts.mulishExtraBold -> R.font.mulish_extrabold
            com.ruuvi.station.app.ui.theme.ruuviStationFonts.mulishRegular -> R.font.mulish_regular
            com.ruuvi.station.app.ui.theme.ruuviStationFonts.oswaldBold -> R.font.oswald_bold
            com.ruuvi.station.app.ui.theme.ruuviStationFonts.oswaldLight -> R.font.oswald_light
            com.ruuvi.station.app.ui.theme.ruuviStationFonts.oswaldRegular -> R.font.oswald_regular
            else -> R.font.mulish_regular
        }

        paint.typeface = ResourcesCompat.getFont(context, fontResId)

        val horizontalPadding = 2f
        val verticalPadding = 2f
        val metrics = paint.fontMetrics
        val width = ceil(paint.measureText(text) + (horizontalPadding * 2)).toInt()
        val height = ceil((metrics.bottom - metrics.top) + (verticalPadding * 2)).toInt()

        if (width <= 0 || height <= 0) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        val baseline = verticalPadding - metrics.top
        canvas.drawText(text, horizontalPadding, baseline, paint)
        return image
    }
}

@Composable
fun CustomFontText(
    text: String,
    fontSize: TextUnit,
    colorProvider: ColorProvider,
    fontFamily: FontFamily,
    modifier: GlanceModifier = GlanceModifier
) {
    if (text.isEmpty()) return
    val context = LocalContext.current
    val bitmap = GlanceFontUtils.createFontBitmap(
        context = context,
        text = text,
        fontSize = fontSize,
        color = colorProvider.getColor(context),
        fontFamily = fontFamily
    )
    Box(modifier = modifier) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = text
        )
    }
}
