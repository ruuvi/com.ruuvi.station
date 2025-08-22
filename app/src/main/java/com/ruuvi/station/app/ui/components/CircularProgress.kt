package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularGradientProgress(
    progress: Float,
    lineColor: Color,
    size: Dp = 130.dp,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {

        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 20f
            val size = this.size.minDimension
            val radius = size / 2
            val startAngle = 135f
            val endAngle = 270f
            val sweepAngle = endAngle * (progress / 100f)
            val rect = Rect(Offset.Zero, Size(size, size))
            val dotRadius = strokeWidth / 2


            // Draw background arc
            drawArc(
                color = Color.Black.copy(alpha = 0.8f),
                startAngle = startAngle,
                sweepAngle = endAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Square)
            )

            // Draw progress arc with gradient
            drawArc(
                color = lineColor.copy(alpha = 0.8f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Square)
            )

            val angleInRadians = Math.toRadians((startAngle + (endAngle * progress / 100f) +
                    (if (progress == 0f) -2.5 else 2.5))).toFloat()
            val dotX = rect.center.x + (radius ) * cos(angleInRadians)
            val dotY = rect.center.y + (radius ) * sin(angleInRadians)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        lineColor,
                        lineColor,
                        lineColor.copy(alpha = 0.8f),
                        lineColor.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                radius = dotRadius * 2.2f,
                center = Offset(dotX, dotY)
                ),
                radius = dotRadius * 2.5f,
                center = Offset(dotX, dotY)
            )

            // Solid inner circle
            drawCircle(
                color = lineColor,
                radius = dotRadius,
                center = Offset(dotX, dotY)
            )
        }

        // Centered Label
        Text(
            text = progress.toInt().toString(),
            fontSize = 54.sp,
            fontFamily = ruuviStationFonts.oswaldBold,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
        ) {
            Text(
                text = "/100",
                fontSize = 18.sp,
                fontFamily = ruuviStationFonts.oswaldRegular,
                color = Color.White
            )
        }
    }
}


@Preview
@Composable
fun CircularGradientProgressPreview0(
    modifier: Modifier = Modifier.background(color = Color(0xFF083C3D))
) {
    CircularGradientProgress(
        progress = 0f,
        lineColor = Color.Red,
        modifier = modifier
    )
}

@Preview
@Composable
fun CircularGradientProgressPreview25(
    modifier: Modifier = Modifier.background(color = Color(0xFF083C3D))
) {
    CircularGradientProgress(
        progress = 25f,
        lineColor = Color.Red,
        modifier = modifier
    )
}

@Preview
@Composable
fun CircularGradientProgressPreview(
    modifier: Modifier = Modifier.background(color = Color(0xFF083C3D))
) {
    CircularGradientProgress(
        progress = 50f,
        lineColor = Color.Yellow,
        modifier = modifier
    )
}

@Preview
@Composable
fun CircularGradientProgressPreview75(
    modifier: Modifier = Modifier.background(color = Color(0xFF083C3D))
) {
    CircularGradientProgress(
        progress = 75f,
        lineColor = Color.Green,
        modifier = modifier
    )
}

@Preview
@Composable
fun CircularGradientProgressPreview100(
    modifier: Modifier = Modifier.background(color = Color(0xFF083C3D))
) {
    CircularGradientProgress(
        progress = 100f,
        lineColor = Color.Green,
        modifier = modifier
    )
}