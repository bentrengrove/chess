package com.bentrengrove.chess

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp

@Composable
fun BoardView(modifier: Modifier = Modifier, board: Board) {
    Column {
        for (y in 0 until 8) {
            Row {
                for (x in 0 until 8) {
                    val white = y % 2 == x % 2
                    val color = if (white) Color.LightGray else Color.DarkGray
                    Box(modifier = Modifier.weight(1f).background(color).aspectRatio(1.0f)) {
                        val position = Position(x, y)
                        val piece = board.pieceAt(position)
                        if (piece != null) {
                            Image(imageResource(id = piece.imageResource()), modifier = Modifier.padding(4.dp).matchParentSize())
                        }
                    }
                }
            }
        }
    }
}