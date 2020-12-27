package com.bentrengrove.chess

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.dp
import com.bentrengrove.chess.ui.ChessTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    var aiEnabled by remember { mutableStateOf(false) }
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

                    if (aiEnabled && game.turn == PieceColor.Black) {
                        GlobalScope.launch {
                            val nextMove = ai.calculateNextMove(game, PieceColor.Black)
                            if (nextMove != null) {
                                val aiResult = game.doMove(nextMove.from, nextMove.to)
                                moveResult = when(aiResult) {
                                    is MoveResult.Success -> aiResult
                                    is MoveResult.Promotion -> aiResult.onPieceSelection(PieceType.Queen)
                                }
                            }
                        }
                    }
                }
            }

            val whiteValue = game.valueFor(PieceColor.White)
            val blackValue = game.valueFor(PieceColor.Black)
            val totalValue = whiteValue + blackValue

            val whitePercentage = ((whiteValue.toFloat()/totalValue.toFloat()) * 100f).roundToInt()
            val blackPercentage = ((blackValue.toFloat()/totalValue.toFloat()) * 100f).roundToInt()
            Column {
                GameView(board = game.board, selection = selection, moves = game.movesForPieceAt(selection), didTap = onSelect)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "State: ${game.gameState}. White: $whitePercentage% Black: $blackPercentage%", style = MaterialTheme.typography.body1)
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Text("Black is AI")
                    Switch(checked = aiEnabled, onCheckedChange = { aiEnabled = it })
                }
                Row {
                    Button(onClick = {
                        val lastMove = game.history.last()
                        val newHistory = game.history.subList(0, game.history.size-1)
                        val newBoard = Board.fromHistory(newHistory)
                        moveResult = MoveResult.Success(Game(newBoard, newHistory))
                        forwardHistory = forwardHistory + listOf(lastMove)
                    }, enabled = game.history.isNotEmpty()) {
                        Text(text = "<")
                    }
                    Button(onClick = {
                        moveResult = MoveResult.Success(Game())
                        forwardHistory = listOf()
                        selection = null
                    }) {
                        Text(text = "New Game")
                    }
                    Button(onClick = {
                        val move = forwardHistory.last()
                        forwardHistory = forwardHistory.subList(0, forwardHistory.size-1)
                        moveResult = game.doMove(move.from, move.to)
                    }, enabled = forwardHistory.isNotEmpty()) {
                        Text(text = ">")
                    }
                }
            }
        }
    }
}