package com.bentrengrove.chess.gamescreen

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bentrengrove.chess.engine.Game
import com.bentrengrove.chess.engine.MoveResult
import com.bentrengrove.chess.engine.Piece
import com.bentrengrove.chess.engine.PieceColor
import com.bentrengrove.chess.engine.PieceType
import com.bentrengrove.chess.engine.Position
import com.bentrengrove.chess.ui.ChessTheme

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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GameView(
    viewModel: GameViewModel = viewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    var selection: Position? by remember { mutableStateOf(null) }

    val moveResult by viewModel.moveResult.collectAsState(initial = MoveResult.Success(Game()))
    val pendingPromotion = moveResult as? MoveResult.Promotion
    val game =
        when (val moveResult = moveResult) {
            is MoveResult.Success -> moveResult.game
            is MoveResult.Promotion -> moveResult.game
        }

    val onSelect: (Position) -> Unit = onSelect@{
        if (pendingPromotion != null) return@onSelect
        val sel = selection
        if (game.canSelect(it)) {
            selection = it
        } else if (sel != null && game.canMove(sel, it)) {
            viewModel.updateResult(game.doMove(sel, it))
            selection = null
            viewModel.clearForwardHistory()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxHeight()) {
            GameView(
                game = game,
                selection = selection,
                moves = game.movesForPieceAt(selection),
                didTap = onSelect,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GameStatusBanner(game = game)
                CapturedPiecesPanel(game = game)
                MoveHistoryList(
                    history = game.history,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (pendingPromotion != null) {
            val onPieceSelection = pendingPromotion.onPieceSelection
            PromotionOverlay(
                color = pendingPromotion.color,
                onPieceSelected = { viewModel.updateResult(onPieceSelection(it)) },
            )
        }
    }
}

private val promotionChoices =
    listOf(
        PieceType.Queen to "Queen",
        PieceType.Rook to "Rook",
        PieceType.Bishop to "Bishop",
        PieceType.Knight to "Knight",
    )

@Composable
internal fun PromotionOverlay(
    color: PieceColor,
    onPieceSelected: (PieceType) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        contentAlignment = Alignment.Center,
    ) {
        PromotionDialogContent(color = color, onPieceSelected = onPieceSelected)
    }
}

@Composable
private fun PromotionDialogContent(
    color: PieceColor,
    onPieceSelected: (PieceType) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Promote Pawn",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for ((type, label) in promotionChoices) {
                    PromotionOption(
                        piece = Piece(id = "promo", type = type, color = color),
                        label = label,
                        onClick = { onPieceSelected(type) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PromotionOption(
    piece: Piece,
    label: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = piece.imageResource()),
                    contentDescription = label,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PromotionDialogWhitePreview() {
    ChessTheme {
        PromotionDialogContent(color = PieceColor.White, onPieceSelected = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PromotionDialogBlackPreview() {
    ChessTheme {
        PromotionDialogContent(color = PieceColor.Black, onPieceSelected = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PromotionDialogDarkPreview() {
    ChessTheme(darkTheme = true) {
        PromotionDialogContent(color = PieceColor.White, onPieceSelected = {})
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun PromotionOverlayOverBoardPreview() {
    ChessTheme {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxHeight()) {
                GameView(
                    game = Game(),
                    selection = null,
                    moves = emptyList(),
                    didTap = {},
                )
            }
            PromotionOverlay(color = PieceColor.White, onPieceSelected = {})
        }
    }
}
