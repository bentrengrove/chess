package com.bentrengrove.chess

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bentrengrove.chess.ui.ChessTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChessTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    GameView()
                }
            }
        }
    }
}

@Composable
fun GameView() {
    val ai by remember { mutableStateOf(AI(PieceColor.Black)) }
    var aiEnabled by remember { mutableStateOf(true) }
    var moveResult by remember { mutableStateOf<MoveResult>(MoveResult.Success(Game()))}
    var selection: Position? by remember { mutableStateOf(null) }
    var forwardHistory by remember { mutableStateOf<List<Move>>(listOf())}

    when (val result = moveResult) {
        is MoveResult.Promotion -> {
            val onPieceSelection = result.onPieceSelection
            val onButtonClicked: (PieceType) -> Unit = {
                moveResult = onPieceSelection(it)
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
            val game = result.game

            val onSelect: (Position) -> Unit = {
                val sel = selection
                if (game.canSelect(it)) {
                    selection = it
                } else if (sel != null && game.canMove(sel, it)) {
                    moveResult = game.doMove(sel, it)
                    selection = null
                    forwardHistory = listOf()
                }
            }

            if (aiEnabled && game.turn == PieceColor.Black) {
                LaunchedEffect(key1 = game) {
                    val nextMove = ai.calculateNextMove(game, PieceColor.Black)
                    if (nextMove != null) {
                        val aiResult = game.doMove(nextMove.from, nextMove.to)
                        moveResult = when (aiResult) {
                            is MoveResult.Success -> aiResult
                            is MoveResult.Promotion -> aiResult.onPieceSelection(PieceType.Queen)
                        }
                    }
                }
            }

            Column {
                CapturedView(pieces = game.capturedPiecesFor(PieceColor.White))
                GameView(game = game, selection = selection, moves = game.movesForPieceAt(selection), didTap = onSelect)
                CapturedView(pieces = game.capturedPiecesFor(PieceColor.Black))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = game.displayGameState, style = MaterialTheme.typography.body1, modifier = Modifier.padding(horizontal = 8.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.CenterHorizontally)) {
                    Button(onClick = {
                        val lastMove = game.history.last()
                        val newHistory = game.history.subList(0, game.history.size - 1)
                        val newBoard = Board.fromHistory(newHistory)
                        moveResult = MoveResult.Success(Game(newBoard, newHistory))
                        forwardHistory = forwardHistory + listOf(lastMove)
                    }, enabled = game.history.isNotEmpty()) {
                        Text(text = "<")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(onClick = {
                        moveResult = MoveResult.Success(Game())
                        forwardHistory = listOf()
                        selection = null
                    }) {
                        Text(text = "New Game")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(onClick = {
                        val move = forwardHistory.last()
                        forwardHistory = forwardHistory.subList(0, forwardHistory.size - 1)
                        moveResult = game.doMove(move.from, move.to)
                    }, enabled = forwardHistory.isNotEmpty()) {
                        Text(text = ">")
                    }
                }
            }
        }
    }
}