package com.bentrengrove.chess.ui

import androidx.compose.ui.graphics.Color

val teal200 = Color(0xFF03DAC5)
val blue800 = Color(0xFF1565c0)
val blueVariant = Color(0xFF003b8e)
val blue300 = Color(0xFF64b5f6)

object BoardColors {
    val lastMoveColor = blueVariant.copy(alpha = 0.5f)
    val lightSquare = Color(0xFFF0D9B5)
    val darkSquare = Color(0xFF946f51)
    val checkColor = Color.Red.copy(alpha = 0.5f)
    val attackColor = Color.Red.copy(alpha = 0.5f)
    val moveColor = lastMoveColor
}
