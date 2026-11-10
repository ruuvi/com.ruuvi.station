package com.ruuvi.station.util.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.TypefaceCompat
import com.ruuvi.station.R
import com.google.android.material.snackbar.Snackbar as ViewSnackbar
import com.ruuvi.station.app.ui.theme.SnackbarBackground
import com.ruuvi.station.app.ui.theme.Titan

private const val DEFAULT_SNACKBAR_DURATION_MILLIS = 2_000
private const val SNACKBAR_CORNER_RADIUS_DP = 6f
private const val SNACKBAR_HORIZONTAL_MARGIN_DP = 12

fun Context.showBottomSnackbar(
    message: CharSequence,
    duration: Int = DEFAULT_SNACKBAR_DURATION_MILLIS,
    actionLabel: CharSequence? = null,
    onAction: (() -> Unit)? = null,
) {
    val activity = findSnackbarActivity() ?: return
    val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
    val layoutRoot = content.getChildAt(0) ?: content

    ViewSnackbar.make(layoutRoot, message, duration).apply {
        setTextColor(Titan.toArgb())
        setActionTextColor(Titan.toArgb())
        setTextMaxLines(4)
        if (actionLabel != null && onAction != null) {
            setAction(actionLabel) { onAction() }
        }

        val font = ResourcesCompat.getFont(context, R.font.mulish_regular)
        val semibold = TypefaceCompat.create(context, font, 600, false)
        view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).typeface = semibold
        view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action).typeface = semibold

        val density = resources.displayMetrics.density
        view.background = GradientDrawable().apply {
            setColor(SnackbarBackground.toArgb())
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
    val fontFamily = FontFamily(Font(R.font.mulish_regular))
    MaterialTheme(
        typography = MaterialTheme.typography.copy(
            body2 = MaterialTheme.typography.body2.copy(
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
            button = MaterialTheme.typography.button.copy(
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        ),
    ) {
        Snackbar(
            modifier = Modifier.padding(horizontal = SNACKBAR_HORIZONTAL_MARGIN_DP.dp),
            action = snackbarData.actionLabel?.let { actionLabel ->
                {
                    TextButton(
                        onClick = snackbarData::performAction,
                        colors = ButtonDefaults.textButtonColors(contentColor = Titan),
                    ) {
                        Text(actionLabel)
                    }
                }
            },
            shape = RoundedCornerShape(SNACKBAR_CORNER_RADIUS_DP.dp),
            backgroundColor = SnackbarBackground,
            contentColor = Titan,
        ) {
            Text(snackbarData.message)
        }
    }
}

private tailrec fun Context.findSnackbarActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext !== this) baseContext.findSnackbarActivity() else null
    else -> null
}
