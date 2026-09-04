package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * These build [Game] directly (not via the [customGame] helper) because en passant legality
 * reads `history.lastOrNull()` - the *real* last move has to be that history's last entry, and
 * customGame's synthetic turn-marker move would clobber it. Conveniently, a genuine pawn
 * double-step as the last move already makes `Game.turn` correct on its own, so no marker is
 * needed here; each test still asserts `turn` up front as a guard rail.
 */
class GameEnPassantTest {
    @Test
    fun whiteCanCaptureEnPassantImmediatelyAfterABlackDoubleStep() {
        val board = customBoard("e5" to "WP0", "d5" to "BP0", "e1" to "WK4", "e8" to "BK4")
        val game = Game(board = board, history = listOf(Move(sq("d7"), sq("d5"))))
        assertEquals(PieceColor.White, game.turn)

        assertTrue(game.enPassantTakePermitted(sq("e5"), sq("d6")))
        assertTrue(game.canMove(sq("e5"), sq("d6")))
    }

    @Test
    fun doMoveExecutingEnPassantRemovesTheCapturedPawn() {
        val board = customBoard("e5" to "WP0", "d5" to "BP0", "e1" to "WK4", "e8" to "BK4")
        val game = Game(board = board, history = listOf(Move(sq("d7"), sq("d5"))))

        val result = (game.doMove(sq("e5"), sq("d6")) as MoveResult.Success).game

        assertEquals(PieceType.Pawn, result.board.pieceAt(sq("d6"))?.type)
        assertEquals(PieceColor.White, result.board.pieceAt(sq("d6"))?.color)
        assertNull(result.board.pieceAt(sq("d5")))
        assertNull(result.board.pieceAt(sq("e5")))
    }

    @Test
    fun blackCanCaptureEnPassantImmediatelyAfterAWhiteDoubleStep() {
        val board = customBoard("d4" to "BP0", "e4" to "WP0", "e1" to "WK4", "e8" to "BK4")
        val game = Game(board = board, history = listOf(Move(sq("e2"), sq("e4"))))
        assertEquals(PieceColor.Black, game.turn)

        val result = (game.doMove(sq("d4"), sq("e3")) as MoveResult.Success).game

        assertEquals(PieceType.Pawn, result.board.pieceAt(sq("e3"))?.type)
        assertNull(result.board.pieceAt(sq("e4")))
    }

    @Test
    fun enPassantIsNotPermittedWhenLastMoveWasNotATwoSquarePawnPush() {
        val board = customBoard("e5" to "WP0", "d5" to "BP0", "e1" to "WK4", "e8" to "BK4")
        val game = Game(board = board, history = listOf(Move(sq("b1"), sq("c3"))))
        assertEquals(PieceColor.White, game.turn)

        assertFalse(game.enPassantTakePermitted(sq("e5"), sq("d6")))
        assertFalse(game.canMove(sq("e5"), sq("d6")))
    }

    @Test
    fun enPassantIsNotPermittedAgainstAOneSquarePawnMove() {
        val board = customBoard("e5" to "WP0", "d6" to "BP0", "e1" to "WK4", "e8" to "BK4")
        val game = Game(board = board, history = listOf(Move(sq("d7"), sq("d6"))))
        assertEquals(PieceColor.White, game.turn)

        assertFalse(game.enPassantTakePermitted(sq("e5"), sq("d6")))
    }

    @Test
    fun enPassantIsNotPermittedAgainstANonPawnPiece() {
        val board = customBoard("e5" to "WP0", "d6" to "BB2", "e1" to "WK4", "e8" to "BK4")
        val game = Game(board = board, history = listOf(Move(sq("d8"), sq("d6"))))
        assertEquals(PieceColor.White, game.turn)

        assertFalse(game.enPassantTakePermitted(sq("e5"), sq("d6")))
    }
}
