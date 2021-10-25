package com.bentrengrove.chess.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = blue300,
    primaryVariant = blueVariant,
    secondary = teal200
)

private val LightColorPalette = lightColors(
    primary = blue800,
    primaryVariant = blueVariant,
    secondary = blue300,
    background = Color(0xFFF5F5F6),
    surface = Color(0xFFE1E2E1),
    onPrimary = Color.White
)

@Composable
fun ChessTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable() () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = typography,
        shapes = shapes,
        content = content
    )
}
