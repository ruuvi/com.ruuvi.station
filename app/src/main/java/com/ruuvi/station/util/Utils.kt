package com.ruuvi.station.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.res.ResourcesCompat
import com.ruuvi.station.R
import com.ruuvi.station.tag.domain.RuuviTag
import timber.log.Timber
import java.util.*

object Utils {
    fun createBall(
        context: Context,
        radius: Int,
        ballColor: Int,
        letterColor: Int,
        deviceId: String,
        letterSize: Float
    ): Bitmap? {
        var letter = getFirstLetter(deviceId)

        val bitmap = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint2 = Paint()
        paint2.color = ballColor
        canvas.drawCircle(radius.toFloat(), radius.toFloat(), radius.toFloat(), paint2)
        val paint = Paint()
        paint.color = letterColor
        paint.textSize = letterSize
        paint.typeface = ResourcesCompat.getFont(context, R.font.mulish_regular)
        val textBounds = Rect()
        paint.getTextBounds(letter, 0, letter.length, textBounds)
        canvas.drawText(
            letter,
            radius - textBounds.exactCenterX(),
            radius - textBounds.exactCenterY(),
            paint
        )
        return bitmap
    }

    fun getFirstLetter(word: String): String {
        var letter = ""
        var multipart = false
        for (part in word) {
            if (!part.isWhitespace()) {
                if (part.isHighSurrogate()) {
                    multipart = true
                    letter += part
                } else {
                    if (multipart) {
                        letter += part
                        if (part.isLowSurrogate()) break
                    } else {
                        letter += part
                        break
                    }
                }
            }
        }
        if (letter.isBlank()) letter = " "
        return letter.uppercase(Locale.getDefault())
    }

    fun getBackground(context: Context, tag: RuuviTag): Bitmap? {
        if (tag.userBackground != null) {
            try {
                val uri = Uri.parse(tag.userBackground)
                return MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } catch (e: Exception) {
                Timber.e("Could not set user background")
            }
        }
        return BitmapFactory.decodeResource(
            context.resources,
            R.drawable.default_background
        )
    }
}