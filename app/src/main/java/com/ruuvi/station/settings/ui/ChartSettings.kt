package com.ruuvi.station.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun ChartSettings(
    scaffoldState: ScaffoldState,
    viewModel: ChartSettingsViewModel
) {
    val displayAllPoint = viewModel.showAllPoints.collectAsState()
    val drawDots = viewModel.drawDots.collectAsState()

    PageSurfaceWithPadding {
        Column() {
            SwitchIndicatorRuuvi(
                text = stringResource(id = R.string.settings_chart_all_points),
                checked = displayAllPoint.value,
                onCheckedChange = viewModel::setShowAllPoints
            )
            Paragraph(text = stringResource(id = R.string.settings_chart_all_points_description))

            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

            SwitchIndicatorRuuvi(
                text = stringResource(id = R.string.settings_chart_draw_dots),
                checked = drawDots.value,
                onCheckedChange = viewModel::setDrawDots
            )
            Paragraph(text = stringResource(id = R.string.settings_chart_draw_dots_description))
        }
    }
}