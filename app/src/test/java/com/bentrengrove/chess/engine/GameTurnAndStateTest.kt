package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class GameTurnAndStateTest {
    @Test
    fun freshGameStartsWithWhiteToMove() {
        assertEquals(PieceColor.White, Game().turn)
    }

    @Test
    fun turnAlternatesAsMovesAreMade() {
        var game = Game()
        assertEquals(PieceColor.White, game.turn)

        game = (game.doMove(sq("e2"), sq("e4")) as MoveResult.Success).game
        assertEquals(PieceColor.Black, game.turn)

        game = (game.doMove(sq("e7"), sq("e5")) as MoveResult.Success).game
        assertEquals(PieceColor.White, game.turn)
    }

    @Test
    fun freshGameStateIsIdle() {
        assertEquals(GameState.IDLE, Game().gameState)
    }

    @Test
    fun displayGameStateReflectsIdleAndWhoseTurn() {
        assertEquals("White's Turn", Game().displayGameState)
    }

    @Test
    fun checkIsDetectedWhenKingCanEscape() {
        // Black king on e8, white rook on e1 with an open file: check, but Kd8/Kf8 escape it.
        val game = customGame(customBoard("e8" to "BK4", "e1" to "WR7", "a1" to "WK4"), PieceColor.Black)

        assertTrue(game.kingIsInCheck(PieceColor.Black))
        assertEquals(GameState.CHECK, game.gameState)
        assertEquals("Check - Black's Turn", game.displayGameState)
    }

    @Test
    fun checkmateInTheCornerIsDetected() {
        // White king boxed in on g1 by its own pawns, black rook checks along the back rank
        // with nothing in between: no legal white move escapes it.
        val board =
            customBoard(
                "g1" to "WK4",
                "f2" to "WP0",
                "g2" to "WP1",
                "h2" to "WP2",
                "a1" to "BR0",
                "e8" to "BK4",
            )
        val game = customGame(board, PieceColor.White)

        assertTrue(game.kingIsInCheck(PieceColor.White))
        assertEquals(GameState.CHECKMATE, game.gameState)
        assertEquals("Checkmate - Black Wins", game.displayGameState)
    }

    @Test
    fun documentsBug_stalemateIsReportedAsIdleInstead() {
        // Textbook king+queen stalemate: black king on a8 has no legal move and isn't in
        // check. Correct chess says GameState.STALEMATE. The engine says IDLE.
        //
        // Why: gameState's escape search does
        //   val newBoard = doMove(it.from, it.to)
        //   (newBoard is Success) && !newBoard.game.kingIsInCheck(color)
        // but doMove already refuses to actually make an unsafe move - it returns
        // Success(oldGame), the board UNCHANGED, rather than Success(newGame) or a
        // non-Success result (see GameCheckSafetyTest). So for every pseudo-legal move that
        // would actually be unsafe, "newBoard.game" is just the current position again, and
        // the redundant safety re-check re-evaluates the CURRENT position's check status
        // instead of the attempted move's. When the current position isn't in check - which
        // is exactly what stalemate means - that re-check always reads "safe", so gameState
        // concludes a legal move exists even though every one of them was silently reverted.
        //
        // This doesn't affect checkmate detection: there, the current (unmoved) position IS
        // already in check, so the same reverted-board re-check still (correctly, if
        // accidentally) reports unsafe. See GameTurnAndStateTest.checkmateInTheCornerIsDetected.
        val board = customBoard("a8" to "BK4", "c7" to "WK4", "b6" to "WQ3")
        val game = customGame(board, PieceColor.Black)

        assertFalse(game.kingIsInCheck(PieceColor.Black))
        assertEquals(GameState.IDLE, game.gameState)
    }

    @Test
    @Ignore(
        "engine bug: doMove reverts unsafe moves to the unchanged board but still returns Success, so " +
            "gameState's escape search re-checks the current (not-in-check) position instead of the attempted " +
            "one - see documentsBug_stalemateIsReportedAsIdleInstead",
    )
    fun spec_stalemateIsDetectedWhenNotInCheckButNoLegalMoves() {
        val board = customBoard("a8" to "BK4", "c7" to "WK4", "b6" to "WQ3")
        val game = customGame(board, PieceColor.Black)

        assertEquals(GameState.STALEMATE, game.gameState)
        assertEquals("Draw - Stalemate", game.displayGameState)
    }

    @Test
    fun kingIsInCheckReturnsFalseWhenThatColorHasNoKingOnBoard() {
        val game = customGame(customBoard("e1" to "WK4"), PieceColor.White)
        assertFalse(game.kingIsInCheck(PieceColor.Black))
    }
}
