package com.ruuvi.station.util.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.google.android.material.snackbar.Snackbar as ViewSnackbar
import com.ruuvi.station.app.ui.theme.Titan
import com.ruuvi.station.app.ui.theme.White

private const val DEFAULT_SNACKBAR_DURATION_MILLIS = 2_000
private const val SNACKBAR_CORNER_RADIUS_DP = 6f
private const val SNACKBAR_HORIZONTAL_MARGIN_DP = 16

fun Context.showBottomSnackbar(
    message: CharSequence,
    duration: Int = DEFAULT_SNACKBAR_DURATION_MILLIS,
    actionLabel: CharSequence? = null,
    onAction: (() -> Unit)? = null,
) {
    val activity = findSnackbarActivity() ?: return
    val content = activity.findViewById<View>(android.R.id.content) ?: return

    ViewSnackbar.make(content, message, duration).apply {
        setTextColor(White.toArgb())
        setActionTextColor(White.toArgb())
        setTextMaxLines(4)
        if (actionLabel != null && onAction != null) {
            setAction(actionLabel) { onAction() }
        }

        val density = resources.displayMetrics.density
        view.background = GradientDrawable().apply {
            setColor(Titan.toArgb())
            cornerRadius = SNACKBAR_CORNER_RADIUS_DP * density
        }
        (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { layoutParams ->
            val horizontalMargin = (SNACKBAR_HORIZONTAL_MARGIN_DP * density).toInt()
            layoutParams.marginStart = horizontalMargin
            layoutParams.marginEnd = horizontalMargin
            view.layoutParams = layoutParams
        }
    }.show()
}

fun Context.showBottomSnackbar(
    @StringRes messageRes: Int,
    duration: Int = DEFAULT_SNACKBAR_DURATION_MILLIS,
    @StringRes actionLabelRes: Int? = null,
    onAction: (() -> Unit)? = null,
) {
    showBottomSnackbar(
        message = getText(messageRes),
        duration = duration,
        actionLabel = actionLabelRes?.let(::getText),
        onAction = onAction,
    )
}

@Composable
fun RuuviSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { snackbarData ->
            RuuviSnackbar(snackbarData)
        },
    )
}

@Composable
private fun RuuviSnackbar(snackbarData: SnackbarData) {
    Snackbar(
        snackbarData = snackbarData,
        modifier = Modifier.padding(horizontal = SNACKBAR_HORIZONTAL_MARGIN_DP.dp),
        shape = RoundedCornerShape(SNACKBAR_CORNER_RADIUS_DP.dp),
        backgroundColor = Titan,
        contentColor = White,
        actionColor = White,
    )
}

private tailrec fun Context.findSnackbarActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext !== this) baseContext.findSnackbarActivity() else null
    else -> null
}
