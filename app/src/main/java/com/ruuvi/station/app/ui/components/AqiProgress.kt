package com.ruuvi.station.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.util.extensions.fixedSp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularGradientProgress(
    progress: Float,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(120.dp) // Adjust size as needed
    ) {

        val infiniteTransition = rememberInfiniteTransition(label = "")

        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = ""
        )

        Canvas(modifier = modifier.size(150.dp)) {
            val strokeWidth = 20f
            val size = this.size.minDimension
            val radius = size / 2 //- strokeWidth / 2
            val center = Offset(size / 2, size / 2)
            val startAngle = 135f  // Starting point for 270-degree arc
            val sweepAngle = 270f * (progress / 100f)
            val rect = Rect(Offset.Zero, Size(size, size))


            // Draw background arc
            drawArc(
                color = Color.Black.copy(alpha = 0.8f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Square)
            )

            // Draw progress arc with gradient
            drawArc(
                color = lineColor,
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Square)
            )

            val angleInRadians = Math.toRadians((135 + (270 * progress / 100f)).toDouble()).toFloat()
            val dotX = rect.center.x + (radius ) * cos(angleInRadians)
            val dotY = rect.center.y + (radius ) * sin(angleInRadians)


            // Draw the glowing dot
            drawCircle(
                color = lineColor.copy(alpha = glowAlpha), // Outer glow effect
                radius = 12f * 1.5f,
                center = Offset(dotX, dotY),
                blendMode = BlendMode.Screen
            )

            drawCircle(
                color = lineColor, // Solid inner circle
                radius = 12f,
                center = Offset(dotX, dotY)
            )

        }
        // Centered Label
        Text(
            text = progress.toInt().toString(),
            fontSize = 60.fixedSp,
            fontFamily = ruuviStationFonts.oswaldBold,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.align(Alignment.BottomCenter))
        {
            Text(
                text = "/100",
                fontSize = 20.fixedSp,
                fontFamily = ruuviStationFonts.oswaldRegular,
                color = Color.White
            )
        }
    }
}
