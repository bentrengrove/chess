package com.bentrengrove.chess

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bentrengrove.chess.gamescreen.GameActions
import com.bentrengrove.chess.gamescreen.GameView
import com.bentrengrove.chess.gamescreen.GameViewModel
import com.bentrengrove.chess.ui.ChessTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChessTheme {
                val navController = rememberNavController()
                val gameViewModel: GameViewModel = viewModel()

                Scaffold(
                    topBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        val canPop = navController.previousBackStackEntry != null

                        val screen = currentDestination?.route?.let { route ->
                            Screen.allMap[route]
                        }

                        val titleText = screen?.title ?: ""
                        val actions = screen?.actions ?: {}

                        TopAppBar(
                            title = { Text(titleText) },
                            navigationIcon = {
                                if (canPop)
                                {
                                    IconButton(onClick = {
                                        navController.popBackStack()
                                    }) {
                                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                                    }
                                }
                            },
                            actions = actions
                        )
                    }
                ) {
                    NavHost(navController = navController, startDestination = Screen.Title.route) {
                        composable(Screen.Title.route) { TitleView(navController, gameViewModel) }
                        composable(Screen.Game.route) { GameView(gameViewModel) }
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String,
                    val title: String,
                    val actions: @Composable RowScope.() -> Unit) {
    object Title: Screen("title", "", actions = {})
    object Game: Screen("game", "", actions = { GameActions() })

    companion object {
        val allList by lazy { listOf(Title, Game) }
        val allMap by lazy { allList.associateBy { it.route } }
    }
}