package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameMiscTest {
    @Test
    fun valueForSumsStandardMaterialAtTheStart() {
        val game = Game()
        // 8 pawns (1) + 2 knights (3) + 2 bishops (3) + 2 rooks (5) + 1 queen (8) + king (0)
        assertEquals(38, game.valueFor(PieceColor.White))
        assertEquals(38, game.valueFor(PieceColor.Black))
    }

    @Test
    fun valueForReflectsCapturedMaterial() {
        val board = customBoard("e1" to "WK4", "e8" to "BK4", "d1" to "WQ3")
        val game = customGame(board, PieceColor.White)
        assertEquals(8, game.valueFor(PieceColor.White))
        assertEquals(0, game.valueFor(PieceColor.Black))
    }

    @Test
    fun capturedPiecesForIsEmptyAtTheStart() {
        assertTrue(Game().capturedPiecesFor(PieceColor.White).isEmpty())
        assertTrue(Game().capturedPiecesFor(PieceColor.Black).isEmpty())
    }

    @Test
    fun capturedPiecesForReportsAMissingPieceById() {
        val board = Board().removePiece(sq("e2"))
        val game = Game(board = board)

        val captured = game.capturedPiecesFor(PieceColor.White)

        assertEquals(1, captured.size)
        assertEquals("WP4", captured.first().id)
        assertEquals(PieceType.Pawn, captured.first().type)
    }

    @Test
    fun capturedPiecesForIgnoresTheOtherColor() {
        val board = Board().removePiece(sq("e2"))
        val game = Game(board = board)
        assertTrue(game.capturedPiecesFor(PieceColor.Black).isEmpty())
    }

    @Test
    fun pieceHasMovedIsFalseWhenHistoryIsEmpty() {
        assertFalse(Game().pieceHasMoved(sq("e2")))
    }

    @Test
    fun pieceHasMovedIsTrueOnceASquareHasAppearedAsAMoveOrigin() {
        val game = Game(history = listOf(Move(sq("e2"), sq("e4"))))
        assertTrue(game.pieceHasMoved(sq("e2")))
        assertFalse(game.pieceHasMoved(sq("e4")))
    }

    @Test
    fun positionIsThreatenedDetectsAPawnAttack() {
        val board = customBoard("e1" to "WK4", "d5" to "BP0")
        val game = customGame(board, PieceColor.White)
        assertTrue(game.positionIsThreatened(sq("e4"), PieceColor.Black))
        assertFalse(game.positionIsThreatened(sq("d4"), PieceColor.Black))
    }

    @Test
    fun pieceIsThreatenedAtIsTrueWhenAnEnemyPieceCanReachTheSquare() {
        val board = customBoard("e1" to "WK4", "e8" to "BR5")
        val game = customGame(board, PieceColor.White)
        assertTrue(game.pieceIsThreatenedAt(sq("e1")))
    }

    @Test
    fun documentsBehavior_pieceIsThreatenedAtIgnoresColorOnEmptySquares() {
        // pieceIsThreatenedAt just asks "can *any* piece move here", with no ownership filter.
        // That's fine for its one real caller (kingIsInCheck, which always targets a square
        // occupied by the king itself - see canMove's own-color guard). But called directly on
        // an *empty* square it also counts your own side's reachability as a "threat", which
        // is not what "threatened" means in chess. Board: only a white knight, empty board
        // otherwise - the square it could hop to reads as "threatened" despite no enemy on the
        // board at all.
        val board = customBoard("b1" to "WN0")
        val game = customGame(board, PieceColor.White)

        assertTrue(game.pieceIsThreatenedAt(sq("c3")))
    }

    @Test
    fun movesForPieceAtIsPseudoLegalAndIncludesMovesThatWalkIntoCheck() {
        // The white king on e1 could step to d1, but a black rook on d8 covers the whole
        // d-file. movesForPieceAt (used to highlight legal squares in the UI) doesn't filter
        // that out - only doMove's check-safety gate does.
        val board = customBoard("e1" to "WK4", "d8" to "BR5")
        val game = customGame(board, PieceColor.White)

        assertTrue(game.movesForPieceAt(sq("e1")).contains(sq("d1")))
        val result = game.doMove(sq("e1"), sq("d1"))
        assertEquals(game, (result as MoveResult.Success).game)
    }
}
