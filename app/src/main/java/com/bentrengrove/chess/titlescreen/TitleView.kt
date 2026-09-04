package com.bentrengrove.chess.titlescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.bentrengrove.chess.Screen
import com.bentrengrove.chess.gamescreen.GameViewModel
import com.bentrengrove.chess.ui.ChessTheme

@Composable
fun TitleView(
    backStack: NavBackStack<NavKey>,
    gameViewModel: GameViewModel,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Chess", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary)
        Spacer(modifier = Modifier.height(32.dp))
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
