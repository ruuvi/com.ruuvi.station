package com.ruuvi.station.app.ui.components

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

// Exact gray levels sampled from the design ("04 - Artboard 5"), keyed by normalised
// distance from the light spot: near-white (240) at the spot down to the flat #696969 base.
private val gradientStops = floatArrayOf(
    0.00f, 0.07f, 0.17f, 0.27f, 0.38f, 0.48f, 0.58f, 0.68f, 0.78f, 0.89f, 1.00f,
)
private val gradientGrays = intArrayOf(
    240, 230, 213, 197, 180, 166, 152, 138, 126, 116, 105,
)
private val gradientColors = IntArray(gradientGrays.size) { index ->
    val gray = gradientGrays[index]
    android.graphics.Color.rgb(gray, gray, gray)
}

// Light spot position and size, as fractions of the layer's width / height.
private const val CENTER_X = 1.0f
private const val CENTER_Y = 0.343f
private const val RADIUS_X = 0.82f
private const val RADIUS_Y = 0.54f

/**
 * Elliptical radial gradient layer drawn over a sensor background image (sensor details
 * and dashboard cards), replacing the old `tag_bg_layer.png` / `tag_bg_layer_dashboard.png`
 * bitmaps. The light spot is centred on the right edge, about a third from the top.
 *
 * The gradient is multiplied onto the image beneath it, so the photo shows through: the
 * dark gray darkens it, the light spot keeps it bright. It is drawn with a dithered native
 * shader so it renders smoothly instead of showing colour banding.
 *
 * Place it directly after the background image in the same parent.
 */
@Composable
fun SensorBackgroundGradientLayer(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.drawWithCache {
            val width = size.width
            val height = size.height
            val center = Offset(x = width * CENTER_X, y = height * CENTER_Y)
            val radiusX = width * RADIUS_X
            val radiusY = height * RADIUS_Y
            val paint = Paint().apply {
                isDither = true
                xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                if (radiusX > 0f && radiusY > 0f) {
                    shader = RadialGradient(
                        center.x,
                        center.y,
                        radiusX,
                        gradientColors,
                        gradientStops,
                        Shader.TileMode.CLAMP,
                    ).apply {
                        // Stretch the circular gradient vertically into an ellipse.
                        setLocalMatrix(
                            Matrix().apply { setScale(1f, radiusY / radiusX, center.x, center.y) }
                        )
                    }
                }
            }
            onDrawBehind {
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawRect(0f, 0f, width, height, paint)
                }
            }
        }
    )
}
