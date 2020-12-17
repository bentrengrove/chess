package com.bentrengrove.chess

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp


@Composable
fun GameView(modifier: Modifier = Modifier, board: Board, selection: Position?, moves: List<Position>, didTap: (Position)->Unit) {
    Box(modifier) {
        BoardBackground(didTap)
        BoardLayout(pieces = board.allPieces, modifier = Modifier.fillMaxWidth().aspectRatio(1.0f))
        MovesView(board, selection, moves)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MovesView(board: Board, selection: Position?, moves: List<Position>) {
    Column {
        for (y in 0 until 8) {
            Row {
                for (x in 0 until 8) {
                    val position = Position(x, y)
                    Box(modifier = Modifier.weight(1f).aspectRatio(1.0f)) {
                        val piece = board.pieceAt(position)
                        val selected = selection != null && position == selection || moves.contains(position)
                        androidx.compose.animation.AnimatedVisibility(visible = selected, modifier = Modifier.matchParentSize(), enter = fadeIn(), exit = fadeOut()) {
                            val color = if (piece != null) Color.Red else Color.Green
                            Box(Modifier.clip(CircleShape).background(color).size(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoardBackground(didTap: (Position)->Unit) {
    Column {
        for (y in 0 until 8) {
            Row {
                for (x in 0 until 8) {
                    val position = Position(x, y)
                    val white = y % 2 == x % 2
                    val color = if (white) Color.LightGray else Color.DarkGray
                    Box(modifier = Modifier.weight(1f).background(color).aspectRatio(1.0f)
                            .clickable(
                                    onClick = { didTap(position) }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PieceView(piece: Piece, modifier: Modifier = Modifier) {
    Image(imageResource(id = piece.imageResource()), modifier = modifier.padding(4.dp))
}

@Composable
private fun BoardLayout(
        modifier: Modifier = Modifier,
        pieces: List<Pair<Position, Piece>>) {
    val constraints = constraintsFor(pieces)
    ConstraintLayout(constraints, modifier) {
        pieces.forEach { (_, piece) ->
            PieceView(piece = piece, modifier = Modifier.layoutId(piece.id))
        }
    }
}

private fun constraintsFor(pieces: List<Pair<Position, Piece>>): ConstraintSet {
    return ConstraintSet {
        val horizontalGuidelines = (0..8).map { createGuidelineFromAbsoluteLeft(it.toFloat() / 8f) }
        val verticalGuidelines = (0..8).map { createGuidelineFromTop(it.toFloat() / 8f) }
        pieces.forEach { (position, piece) ->
            val pieceRef = createRefFor(piece.id)
            constrain(pieceRef) {
                top.linkTo(verticalGuidelines[position.y])
                bottom.linkTo(verticalGuidelines[position.y + 1])
                start.linkTo(horizontalGuidelines[position.x])
                end.linkTo((horizontalGuidelines[position.x + 1]))
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            }
        }
    }
}