package com.bentrengrove.chess.gamescreen

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bentrengrove.chess.engine.Board
import com.bentrengrove.chess.engine.Game
import com.bentrengrove.chess.engine.Move
import com.bentrengrove.chess.engine.Piece
import com.bentrengrove.chess.engine.PieceColor
import com.bentrengrove.chess.engine.Position
import com.bentrengrove.chess.ui.BoardColors

@Composable
fun GameView(modifier: Modifier = Modifier, game: Game, selection: Position?, moves: List<Position>, didTap: (Position) -> Unit) {
    Box(modifier) {
        val board = game.board

        // Highlight king in check, could potentially highlight other things here
        val dangerPositions = listOf(PieceColor.White, PieceColor.Black).mapNotNull { if (game.kingIsInCheck(it)) game.kingPosition(it) else null }

        BoardBackground(game.history.lastOrNull(), selection, dangerPositions, didTap)
        BoardLayout(
            pieces = board.allPieces,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.0f),
        )
        MovesView(board, moves)
    }
}

@Composable
private fun MovesView(board: Board, moves: List<Position>) {
    Column {
        for (y in 0 until 8) {
            Row {
                for (x in 0 until 8) {
                    val position = Position(x, y)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.0f),
                    ) {
                        val piece = board.pieceAt(position)
                        val selected = moves.contains(position)
                        androidx.compose.animation.AnimatedVisibility(visible = selected, modifier = Modifier.matchParentSize(), enter = fadeIn(), exit = fadeOut()) {
                            val color = if (piece != null) BoardColors.attackColor else BoardColors.moveColor
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoardBackground(lastMove: Move?, selection: Position?, dangerPositions: List<Position>, didTap: (Position) -> Unit) {
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(color)
                            .aspectRatio(1.0f)
                            .clickable(
                                onClick = { didTap(position) },
                            ),
                    ) {
                        if (y == 7) {
                            Text(text = "${'a' + x}", modifier = Modifier.align(Alignment.BottomEnd), style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(0.5f))
                        }
                        if (x == 0) {
                            Text(text = "${8 - y}", modifier = Modifier.align(Alignment.TopStart), style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieceView(piece: Piece, modifier: Modifier = Modifier) {
    Image(painterResource(id = piece.imageResource()), modifier = modifier.padding(4.dp), contentDescription = piece.id)
}

val boundsTransform = { _: Rect, _: Rect ->
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = Rect.VisibilityThreshold,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun BoardLayout(
    pieces: List<Pair<Position, Piece>>,
    modifier: Modifier = Modifier,
) {
    val pieceComposables = remember { mutableMapOf<String, @Composable () -> Unit>() }
    LookaheadScope {
        Column(modifier) {
            val pieceModifier = Modifier
                .animateBounds(this@LookaheadScope, boundsTransform = boundsTransform)
                .weight(1f)
                .aspectRatio(1f)

            for (y in 0 until 8) {
                Row {
                    for (x in 0 until 8) {
                        val position = Position(x, y)
                        val piece = pieces.find { it.first == position }?.second

                        if (piece != null) {
                            val pieceComposable = pieceComposables.getOrPut(piece.id) {
                                movableContentOf {
                                    PieceView(
                                        piece = piece,
                                        modifier = pieceModifier,
                                    )
                                }
                            }
                            pieceComposable()
                        } else {
                            Spacer(pieceModifier)
                        }
                    }
                }
            }
        }
    }
}
