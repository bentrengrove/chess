package com.bentrengrove.chess

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.bentrengrove.chess.gamescreen.GameActions
import com.bentrengrove.chess.gamescreen.GameView
import com.bentrengrove.chess.gamescreen.GameViewModel
import com.bentrengrove.chess.titlescreen.TitleView
import com.bentrengrove.chess.ui.ChessTheme
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))

        setContent {
            Content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun Content() {
    ChessTheme {
        val backStack = rememberNavBackStack(Screen.Title)
        val gameViewModel: GameViewModel = viewModel()

        Column {
            val canPop = backStack.size > 1
            val actions: @Composable RowScope.() -> Unit =
                when (backStack.lastOrNull()) {
                    is Screen.Game -> ({ GameActions() })
                    else -> ({})
                }

            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    if (canPop) {
                        IconButton(onClick = {
                            backStack.removeLastOrNull()
                        }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors().copy(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                actions = actions,
            )
            SharedTransitionLayout {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider =
                        entryProvider<NavKey> {
                            entry<Screen.Title> {
                                TitleView(
                                    backStack = backStack,
                                    gameViewModel = gameViewModel,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                                )
                            }
                            entry<Screen.Game> {
                                GameView(
                                    viewModel = gameViewModel,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                                )
                            }
                        },
                )
            }
        }
    }
}

@Serializable
sealed class Screen : NavKey {
    @Serializable
    data object Title : Screen()

    @Serializable
    data object Game : Screen()
}
