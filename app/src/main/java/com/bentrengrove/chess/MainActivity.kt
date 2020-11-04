package com.bentrengrove.chess

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.dp
import androidx.ui.tooling.preview.Preview
import com.bentrengrove.chess.ui.ChessTheme
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
    var game by remember { mutableStateOf(Game())}
    var selection: Position? by remember { mutableStateOf(null) }

    val onSelect: (Position) -> Unit = {
        val sel = selection
        if (game.canSelect(it)) {
            selection = it
        } else if (sel != null && game.canMove(sel, it)) {
            game = game.doMove(sel, it)
            selection = null
            if (aiEnabled) {
                val nextMove = ai.calculateNextMove(game)
                if (nextMove != null) {
                    game = game.doMove(nextMove.from, nextMove.to)
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
        BoardView(board = game.board, selection = selection, moves = game.movesForPieceAt(selection), didTap = onSelect)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "State: ${game.gameState}. White: $whitePercentage% Black: $blackPercentage%", style = MaterialTheme.typography.body1)
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Text("Black is AI")
            Switch(checked = aiEnabled, onCheckedChange = { aiEnabled = it })
        }
        Button(onClick = {
            game = Game()
            selection = null
        }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(text = "Reset")
        }
    }
}