package com.bentrengrove.chess.titlescreen

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.bentrengrove.chess.Screen
import com.bentrengrove.chess.engine.Game
import com.bentrengrove.chess.gamescreen.CHESS_BOARD_SHARED_ELEMENT_KEY
import com.bentrengrove.chess.gamescreen.GameView
import com.bentrengrove.chess.gamescreen.GameViewModel
import com.bentrengrove.chess.ui.ChessTheme

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TitleView(
    backStack: NavBackStack<NavKey>,
    gameViewModel: GameViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Chess", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary)
        Spacer(modifier = Modifier.height(24.dp))

        with(sharedTransitionScope) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = CHESS_BOARD_SHARED_ELEMENT_KEY),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
            ) {
                GameView(
                    game = Game(),
                    selection = null,
                    moves = emptyList(),
                    didTap = {},
                )
                // The decorative board is for show only; swallow taps so it doesn't
                // look like a dead, broken interaction.
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                            ),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        GameButton(
            onClick = { newGame(backStack, gameViewModel, aiEnabled = false) },
            text = "Two Players",
        )
        Spacer(modifier = Modifier.height(16.dp))
        GameButton(
            onClick = { newGame(backStack, gameViewModel, aiEnabled = true) },
            text = "vs Computer",
        )
    }
}

@Composable
private fun GameButton(
    onClick: () -> Unit,
    text: String,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = text, style = MaterialTheme.typography.headlineSmall)
    }
}

@Preview
@Composable
private fun GameButtonPreview() {
    ChessTheme {
        GameButton(onClick = { }, text = "Two Players")
    }
}

private fun newGame(
    backStack: NavBackStack<NavKey>,
    gameViewModel: GameViewModel,
    aiEnabled: Boolean,
) {
    gameViewModel.newGame(aiEnabled)
    backStack.add(Screen.Game)
}
