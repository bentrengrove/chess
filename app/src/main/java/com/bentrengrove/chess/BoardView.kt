package com.bentrengrove.chess

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp

@Composable
fun BoardView(modifier: Modifier = Modifier, board: Board, selection: Position?, moves: List<Position>, didTap: (Position)->Unit) {
    Column {
        for (y in 0 until 8) {
            Row {
                for (x in 0 until 8) {
                    val position = Position(x, y)
                    val white = y % 2 == x % 2
                    val color = if (white) Color.LightGray else Color.DarkGray
                    Box(
                        modifier = Modifier.weight(1f).background(color).aspectRatio(1.0f)
                            .clickable(
                                onClick = { didTap(position) }
                            )
                    ) {
                        val piece = board.pieceAt(position)
                        if (selection != null && position == selection || moves.contains(position)) {
                            Box(Modifier.clip(CircleShape).background(Color.Green).matchParentSize())
                        }
                        if (piece != null) {
                            Image(imageResource(id = piece.imageResource()), modifier = Modifier.padding(4.dp).matchParentSize())
                        }
                    }
                }
            }
        }
    }
}