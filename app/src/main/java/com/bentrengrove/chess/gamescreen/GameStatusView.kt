package com.bentrengrove.chess.gamescreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bentrengrove.chess.engine.Game
import com.bentrengrove.chess.engine.GameState
import com.bentrengrove.chess.engine.Move
import com.bentrengrove.chess.engine.MoveResult
import com.bentrengrove.chess.engine.Piece
import com.bentrengrove.chess.engine.PieceColor
import com.bentrengrove.chess.engine.Position
import com.bentrengrove.chess.ui.ChessTheme

private val checkContainer = Color(0xFFFFCDD2)
private val onCheckContainer = Color(0xFFB71C1C)
private val checkmateContainer = Color(0xFFB71C1C)
private val onCheckmateContainer = Color.White

@Composable
fun GameStatusBanner(
    game: Game,
    modifier: Modifier = Modifier,
) {
    val (container, onContainer) =
        when (game.gameState) {
            GameState.IDLE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            GameState.CHECK -> checkContainer to onCheckContainer
            GameState.CHECKMATE -> checkmateContainer to onCheckmateContainer
            GameState.STALEMATE -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container,
    ) {
        Text(
            text = game.displayGameState,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = onContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
fun CapturedPiecesPanel(
    game: Game,
    modifier: Modifier = Modifier,
) {
    val whiteLost = game.capturedPiecesFor(PieceColor.White)
    val blackLost = game.capturedPiecesFor(PieceColor.Black)
    val materialDiff = whiteLost.sumOf { it.type.value } - blackLost.sumOf { it.type.value }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            CapturedRow(
                label = "White captured",
                pieces = blackLost,
                advantage = (-materialDiff).takeIf { it > 0 },
            )
            CapturedRow(
                label = "Black captured",
                pieces = whiteLost,
                advantage = materialDiff.takeIf { it > 0 },
            )
        }
    }
}

@Composable
private fun CapturedRow(
    label: String,
    pieces: List<Piece>,
    advantage: Int?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        CapturedView(pieces = pieces, modifier = Modifier.weight(1f))
        if (advantage != null) {
            Text(
                text = "+$advantage",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun MoveHistoryList(
    history: List<Move>,
    modifier: Modifier = Modifier,
    startingMoveNumber: Int = 1,
    startingTurn: PieceColor = PieceColor.White,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No moves yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            // A game loaded with Black to move starts with an empty White slot in its first row.
            val paddedHistory = if (startingTurn == PieceColor.Black) listOf(null) + history else history
            val movePairs = paddedHistory.chunked(2)
            // Keep the latest move in view, e.g. after loading a long PGN or stepping through it.
            val listState = rememberLazyListState()
            LaunchedEffect(movePairs.size) { listState.scrollToItem(movePairs.lastIndex) }
            LazyColumn(state = listState, modifier = Modifier.padding(vertical = 4.dp)) {
                itemsIndexed(movePairs) { index, pair ->
                    MoveHistoryRow(
                        moveNumber = startingMoveNumber + index,
                        white = pair.getOrNull(0),
                        black = pair.getOrNull(1),
                    )
                }
            }
        }
    }
}

@Composable
private fun MoveHistoryRow(
    moveNumber: Int,
    white: Move?,
    black: Move?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "$moveNumber.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
        Text(
            text = white?.let { "${it.from.toAlgebraic()} → ${it.to.toAlgebraic()}" } ?: "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = black?.let { "${it.from.toAlgebraic()} → ${it.to.toAlgebraic()}" } ?: "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameStatusBannerIdlePreview() {
    ChessTheme {
        GameStatusBanner(game = Game())
    }
}

private fun sampleGameWithCaptures(): Game {
    var game = Game()
    val moves =
        listOf(
            Position(4, 6) to Position(4, 4), // e2-e4
            Position(3, 1) to Position(3, 3), // d7-d5
            Position(4, 4) to Position(3, 3), // exd5
            Position(2, 1) to Position(2, 2), // c7-c6
            Position(3, 3) to Position(2, 2), // dxc6
        )
    for ((from, to) in moves) {
        game = (game.doMove(from, to) as? MoveResult.Success)?.game ?: game
    }
    return game
}

@Preview(showBackground = true)
@Composable
private fun CapturedPiecesPanelPreview() {
    ChessTheme {
        CapturedPiecesPanel(game = sampleGameWithCaptures())
    }
}

@Preview(showBackground = true)
@Composable
private fun MoveHistoryListPreview() {
    ChessTheme {
        MoveHistoryList(
            history =
                listOf(
                    Move(Position(4, 6), Position(4, 4)),
                    Move(Position(4, 1), Position(4, 3)),
                    Move(Position(6, 7), Position(5, 5)),
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MoveHistoryListLoadedMidGamePreview() {
    ChessTheme {
        val game = Game.fromPgn("[FEN \"4k3/8/8/8/8/8/4P3/4K3 b - - 0 12\"]\n\n12... Kd7 13. e4 Ke6")
        MoveHistoryList(
            history = game.history,
            startingMoveNumber = game.startingFullmoveNumber,
            startingTurn = game.startingTurn,
        )
    }
}
