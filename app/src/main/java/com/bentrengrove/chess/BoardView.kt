package com.bentrengrove.chess

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import com.bentrengrove.chess.ui.BoardColors

@Composable
fun GameView(modifier: Modifier = Modifier, game: Game, selection: Position?, moves: List<Position>, didTap: (Position)->Unit) {
    Box(modifier) {
        val board = game.board

        // Highlight king in check, could potentially highlight other things here
        val dangerPositions = listOf(PieceColor.White, PieceColor.Black).mapNotNull { if (game.kingIsInCheck(it)) game.kingPosition(it) else null }

        val lastMove = game.history.lastOrNull()
        BoardBackground(lastMove, selection, dangerPositions, didTap)
        BoardLayout(pieces = board.allPieces, modifier = Modifier.fillMaxWidth().aspectRatio(1.0f), lastMove = lastMove)
        MovesView(board, moves)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MovesView(board: Board, moves: List<Position>) {
    Column {
        for (y in 0 until 8) {
            Row {
                for (x in 0 until 8) {
                    val position = Position(x, y)
                    Box(modifier = Modifier.weight(1f).aspectRatio(1.0f)) {
                        val piece = board.pieceAt(position)
                        val selected = moves.contains(position)
                        androidx.compose.animation.AnimatedVisibility(visible = selected, modifier = Modifier.matchParentSize(), enter = fadeIn(), exit = fadeOut()) {
                            val color = if (piece != null) BoardColors.attackColor else BoardColors.moveColor
                            Box(Modifier.clip(CircleShape).background(color).size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoardBackground(lastMove: Move?, selection: Position?, dangerPositions: List<Position>, didTap: (Position)->Unit) {
    Column {
        for (y in 0 until 8) {
            Row {
                for (x in 0 until 8) {
                    val position = Position(x, y)
                    val white = y % 2 == x % 2
                    val color = if (lastMove?.contains(position) == true || position == selection) {
                        BoardColors.lastMoveColor
                    } else if (dangerPositions.contains(position)) {
                        BoardColors.checkColor
                    } else {
                        if (white) BoardColors.lightSquare else BoardColors.darkSquare
                    }
                    Box(modifier = Modifier.weight(1f).background(color).aspectRatio(1.0f)
                            .clickable(
                                    onClick = { didTap(position) }
                            )
                    ) {
                        if (y == 7) {
                            Text(text = "${'a'+x}", modifier = Modifier.align(Alignment.BottomEnd), style = MaterialTheme.typography.caption, color = Color.Black.copy(0.5f))
                        }
                        if (x == 0) {
                            Text(text = "${8-y}", modifier = Modifier.align(Alignment.TopStart), style = MaterialTheme.typography.caption, color = Color.Black.copy(0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieceView(piece: Piece, modifier: Modifier = Modifier) {
    Image(imageResource(id = piece.imageResource()), modifier = modifier.padding(4.dp))
}

@Composable
private fun BoardLayout(
        modifier: Modifier = Modifier,
        pieces: List<Pair<Position, Piece>>,
        lastMove: Move?) {
    var lastPieces by remember { mutableStateOf<Map<String, Position>>(mapOf()) }
    var movedPiece by remember { mutableStateOf<Pair<Move, String>?>(null) }

    if (lastMove != null && (lastMove != movedPiece?.first)) {
        val moved = findMovedPiece(pieces, lastPieces)
        if (moved != null) {
            movedPiece = lastMove to moved
        }
    }

    val fromY = lastMove?.from?.y ?: 0
    val toY = (lastMove?.to?.y ?: 0).toFloat()
    var y: Float by remember { mutableStateOf(fromY.toFloat()) }

    LaunchedEffect(movedPiece) {
        animate(fromY.toFloat(), toY) { value, _ ->
            y = value
            Log.d("ANIMATE", "Y update $value")
        }
    }

    val fromX = lastMove?.from?.x ?: 0
    val toX = (lastMove?.to?.x ?: 0).toFloat()
    var x: Float by remember { mutableStateOf(fromX.toFloat()) }

    LaunchedEffect(subject = movedPiece) {
        animate(fromX.toFloat(), toX) { value, _ ->
            x = value
            Log.d("ANIMATE", "X update $value")
        }
    }

    lastPieces = pieces.associateBy { it.second.id }.mapValues { (_, value) -> value.first }

    Log.d("ANIMATE", "$movedPiece $fromY $y")
    val constraints = ConstraintSet {
        val horizontalGuidelines = (0..8).map { createGuidelineFromAbsoluteLeft(it.toFloat() / 8f) }
        val verticalGuidelines = (0..8).map { createGuidelineFromTop(it.toFloat() / 8f) }

        pieces.forEach { (position, piece) ->
            val leftGuide = if (piece.id == movedPiece?.second) createGuidelineFromAbsoluteLeft(x/8f) else horizontalGuidelines[position.x]
            val topGuide = if (piece.id == movedPiece?.second) createGuidelineFromTop(y/8f) else verticalGuidelines[position.y]
            val pieceRef = createRefFor(piece.id)
            constrain(pieceRef) {
                top.linkTo(topGuide)
                start.linkTo(leftGuide)
                width = Dimension.percent(percentFill)
                height = Dimension.percent(percentFill)
            }
        }
    }

    ConstraintLayout(constraints, modifier) {
        pieces.forEach { (_, piece) ->
            PieceView(piece = piece, modifier = Modifier.layoutId(piece.id))
        }
    }
}

private const val percentFill = 1f/8f
@Composable
private fun constraintsFor(pieces: List<Pair<Position, Piece>>, lastMove: Move?) {

}

private fun findMovedPiece(pieces: List<Pair<Position, Piece>>, oldPieces: Map<String, Position>): String? {
    if (oldPieces.isEmpty()) return null
    for (pair in pieces) {
        val position = pair.first
        val pieceId = pair.second.id

        if (oldPieces[pieceId] != position) {
            return pieceId
        }
    }

    return null
}