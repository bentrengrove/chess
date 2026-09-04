package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCheckSafetyTest {
    @Test
    fun doMoveRejectsAMoveThatExposesYourOwnKingToCheck() {
        // The white rook on e2 is pinned to the king by the black rook on e8. Sliding it
        // sideways is geometrically legal for a rook but illegal chess because it opens the
        // e-file onto the king.
        val board = customBoard("e1" to "WK4", "e2" to "WR0", "e8" to "BR5")
        val game = customGame(board, PieceColor.White)

        val result = game.doMove(sq("e2"), sq("d2"))

        assertTrue(result is MoveResult.Success)
        val resultGame = (result as MoveResult.Success).game
        assertEquals(game, resultGame)
        assertEquals(PieceType.Rook, resultGame.board.pieceAt(sq("e2"))?.type)
        assertNull(resultGame.board.pieceAt(sq("d2")))
    }

    @Test
    fun canMoveIsPseudoLegalAndAllowsMovingAPinnedPieceOffTheFile() {
        // canMove only checks a piece's own movement geometry - it has no idea about pins or
        // check. doMove is the layer that actually enforces king safety (see the test above).
        val board = customBoard("e1" to "WK4", "e2" to "WR0", "e8" to "BR5")
        val game = customGame(board, PieceColor.White)

        assertTrue(game.canMove(sq("e2"), sq("d2")))
    }

    @Test
    fun doMoveAllowsMovingOutOfCheckByBlockingTheAttack() {
        val board = customBoard("e1" to "WK4", "d2" to "WR0", "e8" to "BR5")
        val game = customGame(board, PieceColor.White)
        assertTrue(game.kingIsInCheck(PieceColor.White))

        val result = game.doMove(sq("d2"), sq("e2"))

        assertTrue(result is MoveResult.Success)
        val resultGame = (result as MoveResult.Success).game
        assertEquals(PieceType.Rook, resultGame.board.pieceAt(sq("e2"))?.type)
        assertTrue(!resultGame.kingIsInCheck(PieceColor.White))
    }

    @Test
    fun documentsBehavior_doMoveDoesNotValidateMoveGeometryItself() {
        // doMove has no canMove call of its own: it executes whatever from/to it's given and
        // only refuses moves that leave the mover's own king in check. Legality of the move's
        // *shape* is entirely the caller's responsibility (canMove / movesForPieceAt). Here a
        // pawn "moves" from a2 to c4 - not a legal pawn move by any chess rule - and doMove
        // happily executes it because nothing leaves the white king in check.
        val board = customBoard("a2" to "WP0", "e1" to "WK4", "e8" to "BK4")
        val game = customGame(board, PieceColor.White)
        assertTrue(!game.canMove(sq("a2"), sq("c4")))

        val result = game.doMove(sq("a2"), sq("c4"))

        assertTrue(result is MoveResult.Success)
        val resultGame = (result as MoveResult.Success).game
        assertEquals(PieceType.Pawn, resultGame.board.pieceAt(sq("c4"))?.type)
        assertNull(resultGame.board.pieceAt(sq("a2")))
    }
}
