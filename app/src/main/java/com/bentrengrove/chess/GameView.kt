package com.bentrengrove.chess

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class GameViewModel: ViewModel() {
    private var _moveResult = MutableStateFlow<MoveResult>(MoveResult.Success(Game()))
    val moveResult: Flow<MoveResult> get() = _moveResult

    private var forwardHistory = MutableStateFlow<List<Move>>(listOf())

    val canGoBack = moveResult.map {
        val game = (_moveResult.value as? MoveResult.Success)?.game ?: return@map false
        return@map game.history.isNotEmpty()
    }
    val canGoForward = forwardHistory.map { it.isNotEmpty() }

    fun updateResult(result: MoveResult) {
        _moveResult.tryEmit(result)
    }

    fun newGame() {
        updateResult(MoveResult.Success(Game()))
        forwardHistory.tryEmit(listOf())
    }

    fun clearForwardHistory() {
        forwardHistory.tryEmit(listOf())
    }

    fun goBackMove() {
        val game = (_moveResult.value as? MoveResult.Success)?.game ?: return

        val lastMove = game.history.last()
        val newHistory = game.history.subList(0, game.history.size - 1)
        val newBoard = Board.fromHistory(newHistory)
        updateResult(MoveResult.Success(Game(newBoard, newHistory)))
        forwardHistory.tryEmit(forwardHistory.value + listOf(lastMove))
    }

    fun goForwardMove() {
        val game = (_moveResult.value as? MoveResult.Success)?.game ?: return

        val move = forwardHistory.value.last()
        forwardHistory.tryEmit(forwardHistory.value.subList(0, forwardHistory.value.size - 1))
        updateResult(game.doMove(move.from, move.to))
    }
}

@Composable
fun GameActions(viewModel: GameViewModel = viewModel()) {
    val canGoBack by viewModel.canGoBack.collectAsState(initial = false)
    IconButton(onClick = { viewModel.goBackMove() }, enabled = canGoBack) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Undo Move")
    }

    val canGoForward by viewModel.canGoForward.collectAsState(initial = false)
    IconButton(onClick = { viewModel.goForwardMove() }, enabled = canGoForward) {
        Icon(Icons.Filled.ArrowForward, contentDescription = "Redo Move")
    }
}

@Composable
fun GameView(viewModel: GameViewModel = viewModel()) {
    val ai by remember { mutableStateOf(AI(PieceColor.Black)) }
    var aiEnabled by remember { mutableStateOf(false) }
    var selection: Position? by remember { mutableStateOf(null) }

    val moveResult by viewModel.moveResult.collectAsState(initial = MoveResult.Success(Game()))

    when (val moveResult = moveResult) {
        is MoveResult.Promotion -> {
            val onPieceSelection = moveResult.onPieceSelection
            val onButtonClicked: (PieceType) -> Unit = {
                viewModel.updateResult(onPieceSelection(it))
            }
            AlertDialog(
                onDismissRequest = {},
                buttons = {
                    Button({ onButtonClicked(PieceType.Queen) }) { Text(text = "Queen") }
                    Button({ onButtonClicked(PieceType.Rook) }) { Text(text = "Rook") }
                    Button({ onButtonClicked(PieceType.Knight) }) { Text(text = "Knight") }
                    Button({ onButtonClicked(PieceType.Bishop) }) { Text(text = "Bishop") }
                },
                title = {
                    Text(text = "Promote to")
                },
                text = {
                    Text(text = "Please choose a piece type to promote the pawn to")
                }
            )
        }
        is MoveResult.Success -> {
            val game = moveResult.game

            val onSelect: (Position) -> Unit = {
                val sel = selection
                if (game.canSelect(it)) {
                    selection = it
                } else if (sel != null && game.canMove(sel, it)) {
                    viewModel.updateResult(game.doMove(sel, it))
                    selection = null
                    viewModel.clearForwardHistory()
                }
            }

            if (aiEnabled && game.turn == PieceColor.Black) {
                LaunchedEffect(key1 = game) {
                    val nextMove = ai.calculateNextMove(game, PieceColor.Black)
                    if (nextMove != null) {
                        val aiResult = game.doMove(nextMove.from, nextMove.to)
                        val finalAiResult = when (aiResult) {
                            is MoveResult.Success -> aiResult
                            is MoveResult.Promotion -> aiResult.onPieceSelection(PieceType.Queen)
                        }
                        viewModel.updateResult(finalAiResult)
                    }
                }
            }

            Column {
                GameView(
                    game = game,
                    selection = selection,
                    moves = game.movesForPieceAt(selection),
                    didTap = onSelect
                )
                CapturedView(
                    pieces = game.capturedPiecesFor(PieceColor.White),
                    Modifier.fillMaxWidth()
                )
                CapturedView(
                    pieces = game.capturedPiecesFor(PieceColor.Black),
                    Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = game.displayGameState,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}