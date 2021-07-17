package com.bentrengrove.chess

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bentrengrove.chess.ui.ChessTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChessTheme {
                val navController = rememberNavController()
                Scaffold(
                    topBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        val canPop = navController.previousBackStackEntry != null

                        val titleText = currentDestination?.route?.let { route ->
                            Screen.allMap[route]?.title
                        } ?: ""

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
                        )
                    }
                ) {
                    NavHost(navController = navController, startDestination = Screen.Title.route) {
                        composable(Screen.Title.route) { TitleView(navController) }
                        composable(Screen.Game.route) { GameView() }
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String) {
    object Title: Screen("title", "Chess")
    object Game: Screen("game", "")

    companion object {
        val allList by lazy { listOf(Title, Game) }
        val allMap by lazy { allList.associateBy { it.route } }
    }
}