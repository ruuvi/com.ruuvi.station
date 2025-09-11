package com.ruuvi.station.dashboard.ui.dashboard_elements

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.units.domain.score.ExcellentColor
import com.ruuvi.station.units.domain.score.GoodColor
import com.ruuvi.station.units.domain.score.GoodToExcellentColor
import com.ruuvi.station.units.domain.score.ModerateColor
import com.ruuvi.station.units.domain.score.ModerateToGoodColor
import com.ruuvi.station.units.domain.score.PoorColor
import com.ruuvi.station.units.domain.score.PoorToModerateColor
import com.ruuvi.station.units.domain.score.UnhealthyColor
import com.ruuvi.station.units.domain.score.UnhealthyToPoorColor

@Composable
fun GlowingProgressBarIndicator(
    progress: Float,
    thickness: Dp = 4.dp,
    lineColor: Color = Color.Red,
    backgroundColor: Color = Color.Black.copy(alpha = 0.8f),
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(thickness * 3)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val strokeWidth = with(density) { 4.dp.toPx() }

            val dotRadius = strokeWidth / 2
            val progressPosition = size.width * progress
            val y = size.height / 2f

            drawLine(
                color = backgroundColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = thickness.toPx(),
                cap = StrokeCap.Butt
            )

            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(progressPosition, y),
                strokeWidth = thickness.toPx(),
                cap = StrokeCap.Butt
            )


            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        lineColor,
                        lineColor,
                        lineColor.copy(alpha = 0.8f),
                        lineColor.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    radius = dotRadius * 3f,
                    center = Offset(progressPosition, y),
                ),
                radius = dotRadius * 3f,
                center = Offset(progressPosition, y),
            )


            drawCircle(
                color = lineColor,
                radius = dotRadius,
                center = Offset(progressPosition, y)
            )
        }
    }
}

@Preview
@Composable
private fun PreviewGlowingProgressBarIndicator() {
    RuuviTheme {
        Column {
            GlowingProgressBarIndicator(
                progress = 0.05f,
                lineColor = UnhealthyColor,
                modifier = Modifier
                    .background(Color(0xFF083C3D))
                    .width(100.dp)
            )
            GlowingProgressBarIndicator(
                progress = 0.10f,
                lineColor = UnhealthyToPoorColor,
                modifier = Modifier
                    .background(Color(0xFF083C3D))
                    .width(100.dp)
            )
            GlowingProgressBarIndicator(
                progress = 0.35f,
                lineColor = PoorColor,
                modifier = Modifier
                    .background(Color(0xFF083C3D))
                    .width(100.dp)
            )
            GlowingProgressBarIndicator(
                progress = 0.50f,
                lineColor = PoorToModerateColor,
                modifier = Modifier
                    .background(Color(0xFF083C3D))
                    .width(100.dp)
            )
            GlowingProgressBarIndicator(
                progress = 0.60f,
                lineColor = ModerateColor,
                modifier = Modifier
                    .background(Color(0xFF083C3D))
                    .width(100.dp)
            )
            GlowingProgressBarIndicator(
                progress = 0.80f,
                lineColor = ModerateToGoodColor,
                modifier = Modifier
                    .background(Color(0xFF083C3D))
                    .width(100.dp)
            )
            GlowingProgressBarIndicator(
                progress = 0.85f,
                lineColor = GoodColor,
                modifier = Modifier
                    .background(Color(0xFF083C3D))
                    .width(100.dp)
            )
            GlowingProgressBarIndicator(
                progress = 0.90f,
                lineColor = GoodToExcellentColor,
                modifier = Modifier
                    .background(Color(0xFF083C3D))
                    .width(100.dp)
            )
            GlowingProgressBarIndicator(
                progress = 0.95f,
                lineColor = ExcellentColor,
                modifier = Modifier
                    .background(Color(0xFF083C3D))
                    .width(100.dp)
            )
        }
    }
}