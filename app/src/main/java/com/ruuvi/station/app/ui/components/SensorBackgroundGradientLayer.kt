package com.ruuvi.station.app.ui.components

import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.max

/**
 * Which design the overlay reproduces. The dashboard card and the sensor details screen
 * use different layers (formerly `tag_bg_layer_dashboard.png` and `tag_bg_layer.png`).
 */
enum class SensorBackgroundOverlay(internal val design: OverlayDesign) {
    Dashboard(
        OverlayDesign(
            width = 960f,
            height = 1440f,
            layers = listOf(
                RadialLayer(
                    centerX = 1.3121f, centerY = 0.6422f,
                    radiusX = 6.3949f, radiusY = 1.3287f,
                    rotationDegrees = 49.82f,
                    alphas = intArrayOf(2, 1, 13, 46, 87, 123, 151, 175),
                ),
                // Darker band along the top edge.
                LinearLayer(
                    startX = -0.1352f, startY = 0.1207f,
                    endX = -0.0091f, endY = 0.857f,
                    alphas = intArrayOf(149, 108, 72, 41, 18, 2, 1, 2),
                ),
            ),
        )
    ),
    SensorDetails(
        OverlayDesign(
            width = 1500f,
            height = 2800f,
            layers = listOf(
                RadialLayer(
                    centerX = 0.9628f, centerY = 0.6046f,
                    radiusX = 1.1758f, radiusY = 0.8448f,
                    rotationDegrees = 55.30f,
                    alphas = intArrayOf(6, 9, 40, 79, 113, 142, 160, 170),
                ),
                RadialLayer(
                    centerX = 0.8622f, centerY = 0.0439f,
                    radiusX = 3.7276f, radiusY = 1.129f,
                    rotationDegrees = -5.19f,
                    alphas = intArrayOf(170, 118, 66, 25, 23, 42, 74, 102),
                ),
            ),
        )
    ),
}

/**
 * Darkening overlay drawn over a sensor background image so text stays readable, leaving a
 * light spot on the right. It is coded instead of shipping bitmaps, and drawn with dithered
 * shaders so it stays smooth instead of showing colour banding.
 *
 * Each design is a stack of black gradient layers with varying alpha, fitted to the original
 * artwork. Geometry is in units of the design's width (so y = 1.0 is one design-width down),
 * and the design is centre-cropped to the layer's bounds, like `ContentScale.Crop`.
 *
 * Place it directly after the background image in the same parent.
 */
@Composable
fun SensorBackgroundGradientLayer(
    overlay: SensorBackgroundOverlay,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val width = size.width
            val height = size.height
            val paints = if (width > 0f && height > 0f) {
                overlay.design.createPaints(width, height)
            } else {
                emptyList()
            }
            onDrawBehind {
                drawIntoCanvas { canvas ->
                    paints.forEach { canvas.nativeCanvas.drawRect(0f, 0f, width, height, it) }
                }
            }
        }
    )
}

internal class OverlayDesign(
    val width: Float,
    val height: Float,
    val layers: List<OverlayLayer>,
) {
    fun createPaints(viewWidth: Float, viewHeight: Float): List<Paint> {
        // Centre-crop the design into the view; one design-width unit becomes `unit` px.
        val scale = max(viewWidth / width, viewHeight / height)
        val unit = width * scale
        val placement = Matrix().apply {
            setScale(unit, unit)
            postTranslate((viewWidth - width * scale) / 2f, (viewHeight - height * scale) / 2f)
        }
        return layers.map { layer ->
            Paint().apply {
                isDither = true
                shader = layer.createShader(placement)
            }
        }
    }
}

internal sealed class OverlayLayer(alphas: IntArray) {
    protected val colors = IntArray(alphas.size) { android.graphics.Color.argb(alphas[it], 0, 0, 0) }
    protected val stops = FloatArray(alphas.size) { it / (alphas.size - 1f) }

    abstract fun createShader(placement: Matrix): Shader
}

/** Elliptical radial gradient, rotated clockwise by [rotationDegrees] around its centre. */
internal class RadialLayer(
    private val centerX: Float,
    private val centerY: Float,
    private val radiusX: Float,
    private val radiusY: Float,
    private val rotationDegrees: Float,
    alphas: IntArray,
) : OverlayLayer(alphas) {
    override fun createShader(placement: Matrix): Shader =
        RadialGradient(0f, 0f, 1f, colors, stops, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(
                Matrix().apply {
                    setScale(radiusX, radiusY)
                    postRotate(rotationDegrees)
                    postTranslate(centerX, centerY)
                    postConcat(placement)
                }
            )
        }
}

internal class LinearLayer(
    private val startX: Float,
    private val startY: Float,
    private val endX: Float,
    private val endY: Float,
    alphas: IntArray,
) : OverlayLayer(alphas) {
    override fun createShader(placement: Matrix): Shader =
        LinearGradient(startX, startY, endX, endY, colors, stops, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(placement)
        }
}
