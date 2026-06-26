package com.ruuvi.station.widgets.ui.glance

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
