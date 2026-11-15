package com.bentrengrove.chess.screenshots

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.bentrengrove.chess.engine.OpeningTrainer
import com.bentrengrove.chess.openingscreen.LineList
import com.bentrengrove.chess.openingscreen.OpeningListView
import com.bentrengrove.chess.openingscreen.OpeningPracticeView
import com.bentrengrove.chess.openingscreen.sampleDrillResultTrainer
import com.bentrengrove.chess.openingscreen.sampleDrillTrainer
import com.bentrengrove.chess.openingscreen.sampleLearnTrainer
import com.bentrengrove.chess.openingscreen.sampleOpenings
import com.bentrengrove.chess.ui.ChessTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests for the opening trainer screens, in light and dark theme. The trainer states
 * are built from fixed sample openings with a seeded Random, so they render the same every time.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Robolectric's SDK 35+ sandboxes need Java 21, and this project builds with JDK 17.
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class OpeningScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun openingList_light() = captureOpeningList(darkTheme = false)

    @Test
    fun openingList_dark() = captureOpeningList(darkTheme = true)

    @Test
    fun learnWithComment_light() = capturePractice(sampleLearnTrainer(), darkTheme = false)

    @Test
    fun learnWithComment_dark() = capturePractice(sampleLearnTrainer(), darkTheme = true)

    @Test
    fun drillWithHint_light() = capturePractice(sampleDrillTrainer(), darkTheme = false)

    @Test
    fun drillWithHint_dark() = capturePractice(sampleDrillTrainer(), darkTheme = true)

    @Test
    fun drillResultCard_light() = capturePractice(sampleDrillResultTrainer(), darkTheme = false)

    @Test
    fun drillResultCard_dark() = capturePractice(sampleDrillResultTrainer(), darkTheme = true)

    @Test
    fun lineList_light() = captureLineList(darkTheme = false)

    @Test
    fun lineList_dark() = captureLineList(darkTheme = true)

    private fun captureLineList(darkTheme: Boolean) =
        capture(darkTheme) {
            // The bottom sheet's window isn't part of the compose root, so capture its content.
            LineList(lines = sampleLearnTrainer().lines, selectedIndex = 0, onSelectLine = {})
        }

    private fun captureOpeningList(darkTheme: Boolean) =
        capture(darkTheme) {
            OpeningListView(openings = sampleOpenings, onOpeningSelected = {})
        }

    private fun capturePractice(
        trainer: OpeningTrainer,
        darkTheme: Boolean,
    ) = capture(darkTheme) {
        OpeningPracticeView(
            trainer = trainer,
            onMove = { _, _ -> },
            onModeChange = {},
            onSelectLine = {},
            onHint = {},
            onUndo = {},
            onNextLine = {},
            onBack = {},
        )
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
