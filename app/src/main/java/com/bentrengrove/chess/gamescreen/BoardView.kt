package com.bentrengrove.chess.gamescreen

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.bentrengrove.chess.engine.Board
import com.bentrengrove.chess.engine.Game
import com.bentrengrove.chess.engine.Move
import com.bentrengrove.chess.engine.Piece
import com.bentrengrove.chess.engine.PieceColor
import com.bentrengrove.chess.engine.Position
import com.bentrengrove.chess.ui.BoardColors
import com.bentrengrove.chess.ui.ChessTheme

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GameView(
    modifier: Modifier = Modifier,
    game: Game,
    selection: Position?,
    moves: List<Position>,
    didTap: (Position) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    Box(modifier) {
        val board = game.board

        // Highlight king in check, could potentially highlight other things here
        val dangerPositions =
            listOf(
                PieceColor.White,
                PieceColor.Black,
            ).mapNotNull { if (game.kingIsInCheck(it)) game.kingPosition(it) else null }

        BoardBackground(game.history.lastOrNull(), selection, dangerPositions, didTap)
        // Markers sit under the pieces so a capture ring never draws over the piece being captured
        MovesView(board, moves)
        BoardLayout(
            pieces = board.allPieces,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.0f),
        )
    }
}

@Composable
private fun MovesView(
    board: Board,
    moves: List<Position>,
) {
    Column {
        for (y in 0 until 8) {
            Row {
                for (x in 0 until 8) {
                    val position = Position(x, y)
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .aspectRatio(1.0f),
                    ) {
                        val piece = board.pieceAt(position)
                        val selected = moves.contains(position)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = selected,
                            modifier = Modifier.matchParentSize(),
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            MoveMarker(isCapture = piece != null)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Legal-move marker for a single square: a centred dot for a quiet move, or a ring hugging the
 * square's edge for a capture so the captured piece stays fully visible. Both scale with the square.
 */
@Composable
private fun MoveMarker(
    isCapture: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.fillMaxSize()) {
        val squareSize = size.minDimension
        if (isCapture) {
            val strokeWidth = squareSize * CAPTURE_RING_WIDTH_FRACTION
            drawCircle(
                color = BoardColors.attackColor,
                radius = (squareSize - strokeWidth) / 2,
                style = Stroke(width = strokeWidth),
            )
        } else {
            drawCircle(
                color = BoardColors.moveColor,
                radius = squareSize * MOVE_DOT_DIAMETER_FRACTION / 2,
            )
        }
    }
}

private const val MOVE_DOT_DIAMETER_FRACTION = 0.3f
private const val CAPTURE_RING_WIDTH_FRACTION = 0.1f

@Composable
fun BoardBackground(
    lastMove: Move?,
    selection: Position?,
    dangerPositions: List<Position>,
    didTap: (Position) -> Unit,
) {
    Column {
        for (y in 0 until 8) {
            Row {
                for (x in 0 until 8) {
                    val position = Position(x, y)
                    val white = y % 2 == x % 2
                    val color =
                        if (lastMove?.contains(position) == true || position == selection) {
                            BoardColors.lastMoveColor
                        } else if (dangerPositions.contains(position)) {
                            BoardColors.checkColor
                        } else {
                            if (white) BoardColors.lightSquare else BoardColors.darkSquare
                        }
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .background(color)
                                .aspectRatio(1.0f)
                                .clickable(
                                    onClick = { didTap(position) },
                                ),
                    ) {
                        if (y == 7) {
                            Text(
                                text = "${'a' + x}",
                                modifier = Modifier.align(Alignment.BottomEnd),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black.copy(0.5f),
                            )
                        }
                        if (x == 0) {
                            Text(
                                text = "${8 - y}",
                                modifier = Modifier.align(Alignment.TopStart),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black.copy(0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieceView(
    piece: Piece,
    modifier: Modifier = Modifier,
) {
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
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier,
) {
    LookaheadScope {
        Layout(
            modifier = modifier,
            content = {
                for ((_, piece) in pieces) {
                    key(piece.id) {
                        val boundsModifier = Modifier.animateBounds(this@LookaheadScope, boundsTransform = boundsTransform)
                        val pieceModifier =
                            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                with(sharedTransitionScope) {
                                    boundsModifier.sharedElement(
                                        sharedContentState = rememberSharedContentState(key = piece.id),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                    )
                                }
                            } else {
                                boundsModifier
                            }
                        PieceView(
                            piece = piece,
                            modifier = pieceModifier,
                        )
                    }
                }
            },
        ) { measurables, constraints ->
            val squareSize = constraints.maxWidth / 8
            val squareConstraints = Constraints.fixed(squareSize, squareSize)
            val placeables = measurables.map { it.measure(squareConstraints) }
            layout(constraints.maxWidth, constraints.maxWidth) {
                placeables.forEachIndexed { index, placeable ->
                    val (position, _) = pieces[index]
                    placeable.placeRelative(
                        x = position.x * squareSize,
                        y = position.y * squareSize,
                    )
                }
            }
        }
    }
}

/** 1. e4 d5 with the e4 pawn selected: a quiet move to e5 and a capture on d5. */
@Composable
private fun MoveMarkersPreviewContent(darkTheme: Boolean) {
    val game = Game.fromPgn("1. e4 d5")
    val selection = Position(4, 4)
    ChessTheme(darkTheme = darkTheme) {
        GameView(
            game = game,
            selection = selection,
            moves = game.movesForPieceAt(selection),
            didTap = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MoveMarkersPreview() {
    MoveMarkersPreviewContent(darkTheme = false)
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MoveMarkersDarkPreview() {
    MoveMarkersPreviewContent(darkTheme = true)
}
