package com.ruuvi.station.dfu.ui.ui.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@SuppressLint("ConflictingOnColor")
val DarkColorPalette = darkColors(
    primary = Color(0xFF168EA7),
    primaryVariant = Purple700,
    secondary = Teal200,
    background = Color.White,
    surface = Color(0xFF168EA7),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

@SuppressLint("ConflictingOnColor")
val LightColorPalette = lightColors(
    primary = Color(0xFF168EA7),
    primaryVariant = Purple700,
    secondary = Teal200,
    background = Color.White,
    surface = Color(0xFF168EA7),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

@Composable
fun ComruuvistationTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable() () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}