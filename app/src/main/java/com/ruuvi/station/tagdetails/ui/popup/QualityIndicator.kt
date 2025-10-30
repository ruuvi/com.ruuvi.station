package com.ruuvi.station.tagdetails.ui.popup

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.components.limitScaleTo
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.units.domain.score.QualityRange

@Composable
fun QualityIndicator(
    color: Color,
    @StringRes description: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier,
            fontFamily = ruuviStationFonts.mulishBold,
            color = RuuviStationTheme.colors.popupHeaderText,
            fontSize = RuuviStationTheme.fontSizes.miniature.limitScaleTo(1.5f),
            text = stringResource(description),
            maxLines = 1
        )
        Icon(
            imageVector = Icons.Default.Circle,
            contentDescription = "",
            tint = color,
            modifier = Modifier
                .padding(start = RuuviStationTheme.dimensions.medium)
                .size(10.dp)

        )
    }
}

@PreviewLightDark
@Composable
private fun QualityIndicatorPreview() {
    RuuviTheme {
        QualityIndicator(QualityRange.Good.color, QualityRange.Good.description)
    }
}