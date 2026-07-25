package com.ruuvi.station.widgets.ui.glance

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.ruuvi.station.R

fun getZoomFactor(context: Context): Float {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return 1f

    val currentDensityDpi = context.resources.displayMetrics.densityDpi
    val stableDensityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE
    return if (stableDensityDpi > 0) currentDensityDpi.toFloat() / stableDensityDpi.toFloat() else 1f
}

fun TextUnit.toWidgetSp(context: Context): TextUnit {
    val zoomFactor = getZoomFactor(context)
    return (value / zoomFactor).sp
}

fun Dp.toWidgetDp(context: Context): Dp = this / getZoomFactor(context)

internal fun getEffectiveFontScale(
    context: Context,
    referenceFontSizeSp: Float
): Float {
    if (!referenceFontSizeSp.isFinite() || referenceFontSizeSp <= 0f) return 1f

    val metrics = context.resources.displayMetrics
    val scaledPixels = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        referenceFontSizeSp,
        metrics
    )
    val unscaledPixels = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        referenceFontSizeSp,
        metrics
    )
    if (!scaledPixels.isFinite() || !unscaledPixels.isFinite() || unscaledPixels <= 0f) {
        return 1f
    }

    return (scaledPixels / unscaledPixels)
        .takeIf { it.isFinite() && it > 0f }
        ?: 1f
}

internal object WidgetRefreshButtonDefaults {
    val touchTargetSize: Dp = 44.dp
    val iconSize: Dp = 18.dp
    val edgePadding: Dp = 12.dp
    val visualEndInset: Dp = iconSize + edgePadding
    val backingSize: Dp = iconSize + 4.dp
    val backingEndInset: Dp = backingSize + edgePadding
}

@Composable
fun RefreshButton(
    modifier: GlanceModifier = GlanceModifier,
    size: Dp = WidgetRefreshButtonDefaults.touchTargetSize,
    iconSize: Dp = WidgetRefreshButtonDefaults.iconSize,
    paddingBottom: Dp = WidgetRefreshButtonDefaults.edgePadding,
    paddingEnd: Dp = WidgetRefreshButtonDefaults.edgePadding,
    contentAlignment: Alignment = Alignment.BottomEnd,
    backingColor: ColorProvider? = null,
    action: Action
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = GlanceModifier
                .size(size)
                .padding(bottom = paddingBottom, end = paddingEnd)
                .clickable(action),
            contentAlignment = contentAlignment
        ) {
            if (backingColor == null) {
                RefreshIcon(iconSize)
            } else {
                Box(
                    modifier = GlanceModifier
                        .size(WidgetRefreshButtonDefaults.backingSize)
                        .background(backingColor),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    RefreshIcon(iconSize)
                }
            }
        }
    }
}

@Composable
private fun RefreshIcon(iconSize: Dp) {
    Image(
        provider = ImageProvider(R.drawable.ic_widget_d_update),
        contentDescription = null,
        modifier = GlanceModifier.size(iconSize),
        colorFilter = ColorFilter.tint(GlanceColors.refreshButtonColor)
    )
}
