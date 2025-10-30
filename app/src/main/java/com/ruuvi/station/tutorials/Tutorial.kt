package com.ruuvi.station.tutorials

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.Paragraph

sealed interface Tutorial  {
    val preferenceKey: String
    val body: @Composable () -> Unit

    object ChartActionTutorial: Tutorial {
        override val preferenceKey: String = "chartLongTap"
        override val body: @Composable (() -> Unit) = {
            Paragraph(stringResource(R.string.tutorial_chart_long_tap))
        }
    }
}