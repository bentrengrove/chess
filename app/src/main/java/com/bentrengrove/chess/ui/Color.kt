package com.bentrengrove.chess.ui

import androidx.compose.ui.graphics.Color

val purple200 = Color(0xFFBB86FC)
val purple500 = Color(0xFF6200EE)
val purple700 = Color(0xFF3700B3)
val teal200 = Color(0xFF03DAC5)

object BoardColors {
    val lastMoveColor = Color(20,85,30,128)
    val lightSquare = Color(0xFFF0D9B5)
    val darkSquare = Color(0xFF946f51)
    val checkColor = Color.Red.copy(alpha = 0.5f)
    val attackColor = Color.Red.copy(alpha = 0.5f)
    val moveColor = lastMoveColor
}
