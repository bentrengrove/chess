package com.bentrengrove.chess.gamescreen

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.bentrengrove.chess.engine.Game
import com.bentrengrove.chess.engine.PieceColor
import com.bentrengrove.chess.engine.Position
import com.bentrengrove.chess.engine.sq
import com.bentrengrove.chess.ui.ChessTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Taps real screen offsets on the board and checks the square reported matches the one drawn there. */
@RunWith(RobolectricTestRunner::class)
// Robolectric's SDK 35+ sandboxes need Java 21, and this project builds with JDK 17.
@Config(sdk = [34])
class BoardTapTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val tapped = mutableListOf<Position>()

    @Test
    fun whiteOrientation_tapsMapToEngineSquares() {
        showBoard(PieceColor.White)
        tapCell(column = 0, row = 0)
        tapCell(column = 7, row = 7)
        tapCell(column = 4, row = 6)
        assertEquals(listOf(sq("a8"), sq("h1"), sq("e2")), tapped)
    }

    @Test
    fun blackOrientation_tapsMapToFlippedSquares() {
        showBoard(PieceColor.Black)
        tapCell(column = 0, row = 0)
        tapCell(column = 7, row = 7)
        // Black's e7 pawn is drawn on the second row from the bottom, fourth column from the left.
        tapCell(column = 3, row = 6)
        assertEquals(listOf(sq("h1"), sq("a8"), sq("e7")), tapped)
    }

    private fun showBoard(orientation: PieceColor) {
        composeRule.setContent {
            ChessTheme {
                GameView(
                    modifier = Modifier.testTag(BOARD_TAG),
                    game = Game(),
                    selection = null,
                    moves = emptyList(),
                    didTap = { tapped += it },
                    orientation = orientation,
                )
            }
        }
    }

    /** Taps the centre of the on-screen cell at ([column], [row]), counted from the top-left. */
    private fun tapCell(
        column: Int,
        row: Int,
    ) {
        composeRule.onNodeWithTag(BOARD_TAG).performTouchInput {
            val cell = width / 8f
            click(Offset((column + 0.5f) * cell, (row + 0.5f) * cell))
        }
        composeRule.waitForIdle()
    }

    private companion object {
        const val BOARD_TAG = "board"
    }
}
