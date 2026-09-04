package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun stalemateIsDetectedWhenNotInCheckButNoLegalMoves() {
        // Textbook king+queen stalemate: black king on a8 has no legal move and isn't in
        // check. gameState's escape search used to re-check the CURRENT position's check
        // status for every candidate move instead of the attempted move's - doMove already
        // reverts unsafe moves to the unchanged board while still returning Success (see
        // GameCheckSafetyTest), so "was this move safe" needs to ask "did the board actually
        // change", not "is the (unchanged) board currently in check". Fixed by checking
        // newBoard.game != this instead.
        val board = customBoard("a8" to "BK4", "c7" to "WK4", "b6" to "WQ3")
        val game = customGame(board, PieceColor.Black)

        assertFalse(game.kingIsInCheck(PieceColor.Black))
        assertEquals(GameState.STALEMATE, game.gameState)
        assertEquals("Draw - Stalemate", game.displayGameState)
    }

    @Test
    fun documentsBehavior_gameStateMissesAnEscapeThatOnlyExistsViaPromotion() {
        // Same stalemate position as above, plus a black pawn one step from promoting on an
        // unrelated file. The king still has no safe move, but Black is NOT actually
        // stalemated: h2-h1=Q is a fully legal, safe move. gameState still says STALEMATE.
        //
        // Why: gameState's escape search only counts candidates where
        // `doMove(...) is MoveResult.Success` (Game.kt) - a promotion returns
        // MoveResult.Promotion instead, so it's silently excluded from the search regardless
        // of whether it's a real escape. This is pre-existing and separate from the
        // newBoard.game != this fix above; recorded here rather than fixed, since gameState
        // would need to know how to look inside a MoveResult.Promotion (and pick some
        // representative piece type, since the actual choice is a player decision) to close
        // this gap properly.
        val board = customBoard("a8" to "BK4", "c7" to "WK4", "b6" to "WQ3", "h2" to "BP0")
        val game = customGame(board, PieceColor.Black)

        assertFalse(game.kingIsInCheck(PieceColor.Black))
        assertTrue(game.doMove(sq("h2"), sq("h1")) is MoveResult.Promotion)

        // Correct chess: Black has a legal move (the promotion), so this is GameState.IDLE.
        // Current engine output: STALEMATE, because the only escape isn't a Success.
        assertEquals(GameState.STALEMATE, game.gameState)
    }

    @Test
    fun kingIsInCheckReturnsFalseWhenThatColorHasNoKingOnBoard() {
        val game = customGame(customBoard("e1" to "WK4"), PieceColor.White)
        assertFalse(game.kingIsInCheck(PieceColor.Black))
    }
}
