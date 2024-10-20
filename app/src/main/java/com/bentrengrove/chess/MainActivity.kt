package com.bentrengrove.chess

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bentrengrove.chess.gamescreen.GameActions
import com.bentrengrove.chess.gamescreen.GameView
import com.bentrengrove.chess.gamescreen.GameViewModel
import com.bentrengrove.chess.titlescreen.TitleView
import com.bentrengrove.chess.ui.ChessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))

        setContent {
            Content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Content() {
    ChessTheme {
        val navController = rememberNavController()
        val gameViewModel: GameViewModel = viewModel()

        Column {
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
                    if (canPop) {
                        IconButton(onClick = {
                            navController.popBackStack()
                        }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = actions,
            )
            NavHost(
                navController = navController,
                startDestination = Screen.Title.route,
            ) {
                composable(Screen.Title.route) { TitleView(navController, gameViewModel) }
                composable(
                    Screen.Game.route,
                ) {
                    GameView(gameViewModel)
                }
            }
        }
    }
}

sealed class Screen(
    val route: String,
    val title: String,
    val actions: @Composable RowScope.() -> Unit,
) {
    data object Title : Screen("title", "", actions = {})
    data object Game : Screen("game", "", actions = { GameActions() })

    companion object {
        val allList by lazy { listOf(Title, Game) }
        val allMap by lazy { allList.associateBy { it.route } }
    }
}
