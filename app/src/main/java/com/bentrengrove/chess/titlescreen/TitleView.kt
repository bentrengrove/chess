package com.bentrengrove.chess.titlescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bentrengrove.chess.Screen
import com.bentrengrove.chess.gamescreen.GameViewModel
import com.bentrengrove.chess.ui.ChessTheme

@Composable
fun TitleView(navController: NavController, gameViewModel: GameViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.primaryVariant).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Chess", style = MaterialTheme.typography.h2, color = MaterialTheme.colors.onPrimary)
        Spacer(modifier = Modifier.height(32.dp))
        GameButton(
            onClick = { newGame(navController, gameViewModel, aiEnabled = false) },
            text = "Two Players"
        )
        Spacer(modifier = Modifier.height(16.dp))
        GameButton(
            onClick = { newGame(navController, gameViewModel, aiEnabled = true) },
            text = "vs Computer"
        )
    }
}

@Composable
private fun GameButton(onClick: () -> Unit, text: String) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = text, style = MaterialTheme.typography.h4)
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
    navController: NavController,
    gameViewModel: GameViewModel,
    aiEnabled: Boolean
) {
    gameViewModel.newGame(aiEnabled)
    navController.navigate(Screen.Game.route)
}
