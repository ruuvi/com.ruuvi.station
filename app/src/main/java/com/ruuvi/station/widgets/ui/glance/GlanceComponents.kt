package com.ruuvi.station.widgets.ui.glance

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import androidx.annotation.RequiresApi
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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import com.ruuvi.station.R

@RequiresApi(Build.VERSION_CODES.N)
fun getZoomFactor(context: Context): Float {
    val currentDensityDpi = context.resources.displayMetrics.densityDpi
    val stableDensityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE
    return if (stableDensityDpi > 0) currentDensityDpi.toFloat() / stableDensityDpi.toFloat() else 1f
}

@RequiresApi(Build.VERSION_CODES.N)
fun TextUnit.toWidgetSp(context: Context): TextUnit {
    val fontScale = context.resources.configuration.fontScale.takeIf { it > 0f } ?: 1f
    val zoomFactor = getZoomFactor(context)
    return (value / fontScale / zoomFactor).sp
}

@RequiresApi(Build.VERSION_CODES.N)
fun Dp.toWidgetDp(context: Context): Dp = this / getZoomFactor(context)

@Composable
fun RefreshButton(
    modifier: GlanceModifier = GlanceModifier,
    size: Dp = 44.dp,
    iconSize: Dp = 18.dp,
    paddingBottom: Dp = 12.dp,
    paddingEnd: Dp = 12.dp,
    contentAlignment: Alignment = Alignment.Center,
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
            Image(
                provider = ImageProvider(R.drawable.ic_widget_d_update),
                contentDescription = null,
                modifier = GlanceModifier.size(iconSize),
                colorFilter = ColorFilter.tint(GlanceColors.refreshButtonColor)
            )
        }
    }
}
