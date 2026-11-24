package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.theme.Titan
import com.ruuvi.station.app.ui.theme.ToastBackground
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.util.ui.RuuviToast
import com.ruuvi.station.util.ui.showToast
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber

const val SAVE_PROGRESS_TAG = "save_progress_indicator"

/**
 * Shows [UiEvent.ShowToast] messages as toasts, and keeps a progress indicator on screen for as
 * long as a [UiEvent.Progress] operation (such as saving to the cloud) is running.
 */
@Composable
fun ShowStatusToast(uiEvent: SharedFlow<UiEvent>) {
    val context = LocalContext.current
    var progressMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiEvent) {
        uiEvent.collect { uiEvent ->
            when (uiEvent) {
                is UiEvent.ShowToast -> {
                    Timber.d("ShowToast $uiEvent")
                    context.showToast(uiEvent.message.asString(context), uiEvent.duration)
                }
                is UiEvent.Progress -> {
                    progressMessage = uiEvent.message?.asString(context).takeIf { uiEvent.inProgress }
                }
                else -> {}
            }
        }
    }

    progressMessage?.let { SaveProgressIndicator(it) }
}

/**
 * A toast-styled pill with a spinner that stays visible until removed from composition. It sits
 * where [RuuviToast] does and lets touches through to the screen underneath.
 */
@Composable
fun SaveProgressIndicator(message: String) {
    // The popup window can span the whole screen, so the navigation bar and keyboard are
    // added to the margin to keep the indicator above them, where a toast sits.
    val density = LocalDensity.current
    val bottomMargin = with(density) { 16.dp.roundToPx() } +
        WindowInsets.navigationBars.union(WindowInsets.ime).getBottom(density)
    val position = remember(bottomMargin) { BottomOfWindowPosition(bottomMargin) }

    Popup(
        popupPositionProvider = position,
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .background(ToastBackground, RoundedCornerShape(28.dp))
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
                .testTag(SAVE_PROGRESS_TAG),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Titan,
                strokeWidth = 2.dp,
            )
            Text(
                text = message,
                style = TextStyle(
                    color = Titan,
                    fontSize = 14.sp,
                    fontFamily = ruuviStationFonts.mulishRegular,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

/** Centres the popup horizontally at the bottom of the window, whatever its anchor is. */
private class BottomOfWindowPosition(private val bottomMargin: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ) = IntOffset(
        x = (windowSize.width - popupContentSize.width) / 2,
        y = windowSize.height - popupContentSize.height - bottomMargin,
    )
}
