package com.bentrengrove.chess.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColorScheme(
    primary = blue300,
    primaryContainer = blueVariant,
    secondary = teal200,
)

private val LightColorPalette = lightColorScheme(
    primary = blue800,
    primaryContainer = blueVariant,
    secondary = blue300,
    background = Color(0xFFF5F5F6),
    surface = Color(0xFFE1E2E1),
    onPrimary = Color.White,
    onPrimaryContainer = Color.White,
)

@Composable
fun ChessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content:
    @Composable()
    () -> Unit,
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = shapes,
        content = content,
    )
}
