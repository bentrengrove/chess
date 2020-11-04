package com.bentrengrove.chess

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.dp
import androidx.ui.tooling.preview.Preview
import com.bentrengrove.chess.ui.ChessTheme

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
    var game by remember { mutableStateOf(Game())}
    var selection: Position? by remember { mutableStateOf(null) }

    val onSelect: (Position) -> Unit = {
        val sel = selection
        if (game.canSelect(it)) {
            selection = it
        } else if (sel != null && game.canMove(sel, it)) {
            game = game.doMove(sel, it)
            selection = null
        }
    }
    Column {
        BoardView(board = game.board, selection = selection, moves = game.movesForPieceAt(selection), didTap = onSelect)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "State: ${game.gameState}", style = MaterialTheme.typography.body1)
    }
}