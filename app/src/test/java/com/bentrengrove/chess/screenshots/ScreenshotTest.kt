package com.bentrengrove.chess.screenshots

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.rememberNavBackStack
import com.bentrengrove.chess.Screen
import com.bentrengrove.chess.engine.Game
import com.bentrengrove.chess.engine.Move
import com.bentrengrove.chess.engine.PieceColor
import com.bentrengrove.chess.engine.Position
import com.bentrengrove.chess.gamescreen.CapturedPiecesPanel
import com.bentrengrove.chess.gamescreen.GameStatusBanner
import com.bentrengrove.chess.gamescreen.GameView
import com.bentrengrove.chess.gamescreen.GameViewModel
import com.bentrengrove.chess.gamescreen.MoveHistoryList
import com.bentrengrove.chess.gamescreen.PromotionOverlay
import com.bentrengrove.chess.titlescreen.TitleView
import com.bentrengrove.chess.ui.ChessTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * JVM screenshot tests (Roborazzi + Robolectric) for the main composables, in light and dark theme.
 *
 * Baselines live in app/src/test/screenshots/. `./gradlew preflight` verifies against them; after an
 * intentional UI change, re-record with `./gradlew :app:recordRoborazziDebug` and commit the images.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Robolectric's SDK 35+ sandboxes need Java 21, and this project builds with JDK 17.
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun titleScreen_light() = captureTitleScreen(darkTheme = false)

    @Test
    fun titleScreen_dark() = captureTitleScreen(darkTheme = true)

    @Test
    fun boardStartingPosition_light() = captureBoard(darkTheme = false)

    @Test
    fun boardStartingPosition_dark() = captureBoard(darkTheme = true)

    @Test
    fun boardMoveMarkers_light() = captureBoardMoveMarkers(darkTheme = false)

    @Test
    fun boardMoveMarkers_dark() = captureBoardMoveMarkers(darkTheme = true)

    @Test
    fun promotionPicker_light() = capturePromotionPicker(darkTheme = false)

    @Test
    fun promotionPicker_dark() = capturePromotionPicker(darkTheme = true)

    @Test
    fun gameStatusView_light() = captureGameStatusView(darkTheme = false)

    @Test
    fun gameStatusView_dark() = captureGameStatusView(darkTheme = true)

    @OptIn(ExperimentalSharedTransitionApi::class)
    private fun captureTitleScreen(darkTheme: Boolean) {
        val gameViewModel = GameViewModel()
        capture(darkTheme) {
            // TitleView takes part in the title-to-game shared element transition, so give it the
            // scopes it would get from the NavDisplay. Infinite animations (the falling pieces) are
            // paused by the compose test rule, so the capture is deterministic.
            SharedTransitionLayout {
                AnimatedVisibility(visible = true) {
                    TitleView(
                        backStack = rememberNavBackStack(Screen.Title),
                        gameViewModel = gameViewModel,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this,
                    )
                }
            }
        }
    }

    private fun captureBoard(darkTheme: Boolean) =
        capture(darkTheme) {
            GameView(game = Game(), selection = null, moves = emptyList(), didTap = {})
        }

    private fun captureBoardMoveMarkers(darkTheme: Boolean) =
        capture(darkTheme) {
            // After 1. e4 d5 the e4 pawn can move to e5 (dot) or capture on d5 (ring).
            val game = Game.fromPgn("1. e4 d5")
            val selection = Position(4, 4)
            GameView(game = game, selection = selection, moves = game.movesForPieceAt(selection), didTap = {})
        }

    private fun capturePromotionPicker(darkTheme: Boolean) =
        capture(darkTheme) {
            // Mirrors GameView's layout while a promotion is pending: the picker over the board.
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxHeight()) {
                    GameView(game = Game(), selection = null, moves = emptyList(), didTap = {})
                }
                PromotionOverlay(color = PieceColor.White, onPieceSelected = {})
            }
        }

    private fun captureGameStatusView(darkTheme: Boolean) =
        capture(darkTheme) {
            val capturesGame = Game.fromPgn("1. e4 d5 2. exd5 c6 3. dxc6")
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GameStatusBanner(game = Game())
                GameStatusBanner(game = Game.fromPgn("1. e4 d5 2. Bb5+"))
                GameStatusBanner(game = Game.fromPgn("1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7#"))
                GameStatusBanner(game = Game.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"))
                CapturedPiecesPanel(game = capturesGame)
                MoveHistoryList(
                    history =
                        listOf(
                            Move(Position(4, 6), Position(4, 4)),
                            Move(Position(4, 1), Position(4, 3)),
                            Move(Position(6, 7), Position(5, 5)),
                        ),
                )
            }
        }

    private fun capture(
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            ChessTheme(darkTheme = darkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    content()
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
