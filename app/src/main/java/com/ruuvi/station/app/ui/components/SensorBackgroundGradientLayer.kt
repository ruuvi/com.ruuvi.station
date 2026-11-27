package com.ruuvi.station.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import coil.size.Scale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt

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
 * Background image with the overlay drawn on top of it, in a single pass.
 *
 * Drawing the overlay as its own translucent layer means the GPU can only dither the overlay's
 * alpha before it is blended, and on a dark or flat background that dither shrinks to a
 * fraction of a colour level, so 1-level steps show as bands. Composing the image and the
 * overlay into one shader lets Skia compute the final opaque colour at full precision and
 * dither *that*, which hides the steps.
 *
 * Before Android 9 (API 28) hardware rendering cannot draw a ComposeShader that nests another
 * one or combines two shaders of the same type, so there the image and each layer are drawn
 * one after another instead (see [OverlayDesign.createPaints]).
 */
@Composable
fun SensorBackgroundImage(
    model: Any?,
    overlay: SensorBackgroundOverlay,
    modifier: Modifier = Modifier,
    onError: (Throwable) -> Unit = {},
) {
    val context = LocalContext.current
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var background by remember(model) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(model, viewSize) {
        if (model == null || viewSize == IntSize.Zero) return@LaunchedEffect
        val request = backgroundImageRequest(context, model, viewSize)
        when (val result = context.imageLoader.execute(request)) {
            is SuccessResult -> background = (result.drawable as? BitmapDrawable)?.bitmap
            is ErrorResult -> onError(result.throwable)
        }
    }

    Spacer(
        modifier = modifier
            .onSizeChanged { viewSize = it }
            .drawWithCache {
                val width = size.width
                val height = size.height
                val paints = if (width > 0f && height > 0f) {
                    overlay.design.createPaints(width, height, background)
                } else {
                    emptyList()
                }
                onDrawBehind {
                    drawIntoCanvas { canvas ->
                        paints.forEach { paint -> canvas.nativeCanvas.drawRect(0f, 0f, width, height, paint) }
                    }
                }
            }
    )
}

/**
 * Loads [model] for a view of [viewSize]. Inexact precision lets Coil decode a photo smaller
 * than the view at its own size instead of enlarging it: the shader already scales the image
 * to fill the view, so an 800x600 photo would otherwise become a ~4160x3120 bitmap (~52 MB) on
 * a 1440x3120 screen. With an explicit size and no view target, Coil's default is exact.
 */
internal fun backgroundImageRequest(context: Context, model: Any, viewSize: IntSize): ImageRequest =
    ImageRequest.Builder(context)
        .data(model)
        .size(viewSize.width, viewSize.height)
        .scale(Scale.FILL)
        .precision(Precision.INEXACT)
        // A software bitmap can be sampled by a BitmapShader on every API level.
        .allowHardware(false)
        .build()

internal class OverlayDesign(
    val width: Float,
    val height: Float,
    val layers: List<OverlayLayer>,
) {
    /**
     * The paints that draw [background] (if any) and the overlay over the whole view, bottom
     * first.
     *
     * From API 28 this is one paint for the whole stack. The layers are composed inside a
     * single shader, so they are blended at full precision and quantised (and dithered) once,
     * instead of each layer being rounded to 8 bits on its own and the rounding steps adding up.
     *
     * Hardware rendering before API 28 supports neither a ComposeShader nested in another nor
     * one that combines two shaders of the same type (such as two radial gradients), so on
     * older versions each shader gets a paint of its own. That rounds each layer separately,
     * which can leave faint banding on dark backgrounds, but draws correctly.
     */
    fun createPaints(
        viewWidth: Float,
        viewHeight: Float,
        background: Bitmap? = null,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): List<Paint> {
        // Centre-crop the design into the view; one design-width unit becomes `unit` px.
        val scale = max(viewWidth / width, viewHeight / height)
        val unit = width * scale
        val placement = Matrix().apply {
            setScale(unit, unit)
            postTranslate((viewWidth - width * scale) / 2f, (viewHeight - height * scale) / 2f)
        }
        val backgroundShader = background?.let { createCropShader(it, viewWidth, viewHeight) }
        val layerShaders = layers.map { it.createShader(placement) }
        if (sdkInt < Build.VERSION_CODES.P) {
            return (listOfNotNull(backgroundShader) + layerShaders).map(::paintWith)
        }
        val overlayShader = layerShaders.reduce { below, above ->
            ComposeShader(below, above, PorterDuff.Mode.SRC_OVER)
        }
        val shader = if (backgroundShader == null) {
            overlayShader
        } else {
            ComposeShader(backgroundShader, overlayShader, PorterDuff.Mode.SRC_OVER)
        }
        return listOf(paintWith(shader))
    }

    private fun paintWith(shader: Shader) = Paint().apply {
        isDither = true
        isFilterBitmap = true
        this.shader = shader
    }

    /** Like `ContentScale.Crop`: fill the view, keep the aspect ratio, centre the overflow. */
    private fun createCropShader(bitmap: Bitmap, viewWidth: Float, viewHeight: Float): Shader {
        val scale = max(viewWidth / bitmap.width, viewHeight / bitmap.height)
        return BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(
                Matrix().apply {
                    setScale(scale, scale)
                    postTranslate(
                        (viewWidth - bitmap.width * scale) / 2f,
                        (viewHeight - bitmap.height * scale) / 2f,
                    )
                }
            )
        }
    }
}

internal sealed class OverlayLayer(alphas: IntArray, mirrorAtStart: Boolean) {
    private val curve = smoothedAlphaCurve(alphas, mirrorAtStart)

    /** Where the stored curve begins and ends, in units of the design gradient's length. */
    protected val domainStart = curve.start
    protected val domainEnd = curve.end

    protected val stops = FloatArray(curve.values.size) { it / (curve.values.size - 1f) }
    private val stopAlphas = FloatArray(curve.values.size) { (curve.values[it] / 255f).coerceIn(0f, 1f) }

    // Float colours (API 29+) avoid rounding every stop to 8 bits, which would add tiny
    // slope wobbles of its own; older versions fall back to 8-bit colours.
    protected val longColors: LongArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        LongArray(stopAlphas.size) { android.graphics.Color.pack(0f, 0f, 0f, stopAlphas[it]) }
    } else {
        null
    }
    protected val intColors = IntArray(stopAlphas.size) {
        android.graphics.Color.argb((stopAlphas[it] * 255f).roundToInt(), 0, 0, 0)
    }

    abstract fun createShader(placement: Matrix): Shader
}

/** Gaussian smoothing width, as a fraction of the gradient's length (radius or start-to-end). */
internal const val SMOOTHING = 0.07f

/** Stops handed to the GPU; it joins them with straight lines, so they must be fine enough. */
private const val CURVE_STOPS = 192

internal class SmoothedCurve(val start: Float, val end: Float, val values: FloatArray)

/**
 * The design's alpha profile, Gaussian-smoothed along the gradient.
 *
 * The design is a few evenly spaced stops joined by straight lines and clamped flat beyond its
 * ends. Every corner of that profile - at each stop and above all where it stops at its edge -
 * is a sudden change of slope, which the eye picks up as a line even when there is no
 * quantisation banding. Smoothing spreads each corner over a wide area, leaves the straight
 * stretches as they are, and settles to exactly the design's end value beyond the edge, so the
 * overlay looks the same but has nowhere for a line to form.
 *
 * The returned curve covers [SmoothedCurve.start]..[SmoothedCurve.end] in units of the design
 * gradient's length, reaching past the design's ends so the smoothing can finish before the
 * shader clamps. With [mirrorAtStart] (a radial gradient) the profile is mirrored at 0, so the
 * centre stays smooth too and the curve starts at 0.
 */
internal fun smoothedAlphaCurve(
    alphas: IntArray,
    mirrorAtStart: Boolean,
    smoothing: Float = SMOOTHING,
    stopCount: Int = CURVE_STOPS,
): SmoothedCurve {
    val reach = 4f * smoothing
    val start = if (mirrorAtStart) 0f else -reach
    val end = 1f + reach
    if (alphas.size < 2 || smoothing <= 0f) {
        return SmoothedCurve(0f, 1f, FloatArray(alphas.size) { alphas[it].toFloat() })
    }
    val segments = alphas.size - 1
    fun design(u: Float): Float {
        val t = (if (mirrorAtStart) abs(u) else u).coerceIn(0f, 1f) * segments
        val k = t.toInt().coerceAtMost(segments - 1)
        val f = t - k
        return alphas[k] * (1f - f) + alphas[k + 1] * f
    }
    val taps = 81
    val weights = FloatArray(taps) {
        val x = (it - taps / 2) / (taps / 2f) * 4f // -4σ..4σ
        exp(-0.5f * x * x)
    }
    val weightSum = weights.sum()
    val tapStep = 4f * smoothing / (taps / 2)
    val values = FloatArray(stopCount) { i ->
        val t = start + (end - start) * i / (stopCount - 1f)
        var sum = 0f
        for (j in 0 until taps) sum += weights[j] * design(t + (j - taps / 2) * tapStep)
        sum / weightSum
    }
    return SmoothedCurve(start, end, values)
}

/** Elliptical radial gradient, rotated clockwise by [rotationDegrees] around its centre. */
internal class RadialLayer(
    private val centerX: Float,
    private val centerY: Float,
    private val radiusX: Float,
    private val radiusY: Float,
    private val rotationDegrees: Float,
    alphas: IntArray,
) : OverlayLayer(alphas, mirrorAtStart = true) {
    override fun createShader(placement: Matrix): Shader {
        val gradient = longColors?.let { RadialGradient(0f, 0f, 1f, it, stops, Shader.TileMode.CLAMP) }
            ?: RadialGradient(0f, 0f, 1f, intColors, stops, Shader.TileMode.CLAMP)
        return gradient.apply {
            setLocalMatrix(
                Matrix().apply {
                    // The stored curve reaches past the design radius by `domainEnd`.
                    setScale(radiusX * domainEnd, radiusY * domainEnd)
                    postRotate(rotationDegrees)
                    postTranslate(centerX, centerY)
                    postConcat(placement)
                }
            )
        }
    }
}

internal class LinearLayer(
    private val startX: Float,
    private val startY: Float,
    private val endX: Float,
    private val endY: Float,
    alphas: IntArray,
) : OverlayLayer(alphas, mirrorAtStart = false) {
    override fun createShader(placement: Matrix): Shader {
        // The stored curve reaches past both design ends.
        val x0 = startX + (endX - startX) * domainStart
        val y0 = startY + (endY - startY) * domainStart
        val x1 = startX + (endX - startX) * domainEnd
        val y1 = startY + (endY - startY) * domainEnd
        val gradient = longColors?.let { LinearGradient(x0, y0, x1, y1, it, stops, Shader.TileMode.CLAMP) }
            ?: LinearGradient(x0, y0, x1, y1, intColors, stops, Shader.TileMode.CLAMP)
        return gradient.apply { setLocalMatrix(placement) }
    }
}
