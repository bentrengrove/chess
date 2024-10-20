package com.bentrengrove.chess.gamescreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bentrengrove.chess.engine.Game
import com.bentrengrove.chess.engine.MoveResult
import com.bentrengrove.chess.engine.PieceColor
import com.bentrengrove.chess.engine.PieceType
import com.bentrengrove.chess.engine.Position

@Composable
fun GameActions(viewModel: GameViewModel = viewModel()) {
    val canGoBack by viewModel.canGoBack.collectAsState(initial = false)
    IconButton(onClick = { viewModel.goBackMove() }, enabled = canGoBack) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Undo Move")
    }

    val canGoForward by viewModel.canGoForward.collectAsState(initial = false)
    IconButton(onClick = { viewModel.goForwardMove() }, enabled = canGoForward) {
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Redo Move")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameView(viewModel: GameViewModel = viewModel()) {
    var selection: Position? by remember { mutableStateOf(null) }

    val moveResult by viewModel.moveResult.collectAsState(initial = MoveResult.Success(Game()))

    when (val moveResult = moveResult) {
        is MoveResult.Promotion -> {
            val onPieceSelection = moveResult.onPieceSelection
            val onButtonClicked: (PieceType) -> Unit = {
                viewModel.updateResult(onPieceSelection(it))
            }
            BasicAlertDialog(onDismissRequest = {}) {
                Surface {
                    Column {
                        Text(text = "Promote to")
                        Button({ onButtonClicked(PieceType.Queen) }) { Text(text = "Queen") }
                        Button({ onButtonClicked(PieceType.Rook) }) { Text(text = "Rook") }
                        Button({ onButtonClicked(PieceType.Knight) }) { Text(text = "Knight") }
                        Button({ onButtonClicked(PieceType.Bishop) }) { Text(text = "Bishop") }
                    }
                }
            }
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

            Column(Modifier.fillMaxHeight()) {
                GameView(
                    game = game,
                    selection = selection,
                    moves = game.movesForPieceAt(selection),
                    didTap = onSelect,
                )
                CapturedView(
                    pieces = game.capturedPiecesFor(PieceColor.White),
                    Modifier.fillMaxWidth(),
                )
                CapturedView(
                    pieces = game.capturedPiecesFor(PieceColor.Black),
                    Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = game.displayGameState,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}
