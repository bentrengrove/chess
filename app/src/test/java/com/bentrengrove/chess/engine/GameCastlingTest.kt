package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class GameCastlingTest {
    @Test
    fun kingSideCastlingIsPermittedWithClearUnmovedPieces() {
        val game = customGame(customBoard("e1" to "WK4", "h1" to "WR7", "e8" to "BK4"), PieceColor.White)
        assertTrue(game.castlingPermitted(sq("e1"), sq("g1")))
    }

    @Test
    fun queenSideCastlingIsPermittedWithClearUnmovedPieces() {
        val game = customGame(customBoard("e1" to "WK4", "a1" to "WR0", "e8" to "BK4"), PieceColor.White)
        assertTrue(game.castlingPermitted(sq("e1"), sq("c1")))
    }

    @Test
    fun doMoveExecutesKingSideCastlingByMovingBothKingAndRook() {
        val game = customGame(customBoard("e1" to "WK4", "h1" to "WR7", "e8" to "BK4"), PieceColor.White)
        val result = (game.doMove(sq("e1"), sq("g1")) as MoveResult.Success).game

        assertEquals(PieceType.King, result.board.pieceAt(sq("g1"))?.type)
        assertEquals(PieceType.Rook, result.board.pieceAt(sq("f1"))?.type)
        assertNull(result.board.pieceAt(sq("e1")))
        assertNull(result.board.pieceAt(sq("h1")))
    }

    @Test
    fun doMoveExecutesQueenSideCastlingByMovingBothKingAndRook() {
        val game = customGame(customBoard("e1" to "WK4", "a1" to "WR0", "e8" to "BK4"), PieceColor.White)
        val result = (game.doMove(sq("e1"), sq("c1")) as MoveResult.Success).game

        assertEquals(PieceType.King, result.board.pieceAt(sq("c1"))?.type)
        assertEquals(PieceType.Rook, result.board.pieceAt(sq("d1"))?.type)
        assertNull(result.board.pieceAt(sq("e1")))
        assertNull(result.board.pieceAt(sq("a1")))
    }

    @Test
    fun castlingIsBlockedWhenASquareBetweenKingAndRookIsOccupied() {
        val game =
            customGame(
                customBoard("e1" to "WK4", "a1" to "WR0", "b1" to "WB2", "e8" to "BK4"),
                PieceColor.White,
            )
        assertFalse(game.castlingPermitted(sq("e1"), sq("c1")))
    }

    @Test
    fun castlingIsBlockedOnceTheKingHasMovedEvenIfItReturned() {
        val board = customBoard("e1" to "WK4", "h1" to "WR7", "e8" to "BK4")
        val game = customGame(board, PieceColor.White, history = listOf(Move(sq("e1"), sq("f1"))))
        assertFalse(game.castlingPermitted(sq("e1"), sq("g1")))
    }

    @Test
    fun castlingIsBlockedOnceTheRelevantRookHasMoved() {
        val board = customBoard("e1" to "WK4", "h1" to "WR7", "e8" to "BK4")
        val game = customGame(board, PieceColor.White, history = listOf(Move(sq("h1"), sq("h4"))))
        assertFalse(game.castlingPermitted(sq("e1"), sq("g1")))
    }

    @Test
    fun rookMovingDoesNotBlockCastlingOnTheOtherSide() {
        val board = customBoard("e1" to "WK4", "a1" to "WR0", "h1" to "WR7", "e8" to "BK4")
        val game = customGame(board, PieceColor.White, history = listOf(Move(sq("h1"), sq("h4"))))
        assertTrue(game.castlingPermitted(sq("e1"), sq("c1")))
    }

    @Test
    fun castlingThroughAnAttackedSquareIsBlocked() {
        // Black rook on f8 attacks f1 down the open f-file; the king cannot pass through it
        // on the way to g1. The rook is placed before the king in board-scan order so this
        // exercises the real threat correctly - see PositionIsThreatenedBugTest for the
        // scan-order bug in Game.positionIsThreatened that this test deliberately avoids.
        val board = customBoard("e1" to "WK4", "h1" to "WR7", "f8" to "BR5", "g8" to "BK4")
        val game = customGame(board, PieceColor.White)

        assertTrue(game.positionIsThreatened(sq("f1"), PieceColor.Black))
        assertFalse(game.castlingPermitted(sq("e1"), sq("g1")))
    }

    @Test
    fun castlingOutOfCheckIsBlocked() {
        // Black rook checks the white king on its home square down the open e-file; can't
        // castle while in check. The rook is placed before the black king in board-scan
        // order for the same reason as castlingThroughAnAttackedSquareIsBlocked above.
        val board = customBoard("e1" to "WK4", "h1" to "WR7", "e3" to "BR5", "a1" to "BK4")
        val game = customGame(board, PieceColor.White)

        assertTrue(game.positionIsThreatened(sq("e1"), PieceColor.Black))
        assertFalse(game.castlingPermitted(sq("e1"), sq("g1")))
    }

    // --- Black side (kingsRow = 0) ---

    @Test
    fun blackKingSideCastlingIsPermittedWithClearUnmovedPieces() {
        val game = customGame(customBoard("e8" to "BK4", "h8" to "BR7", "e1" to "WK4"), PieceColor.Black)
        assertTrue(game.castlingPermitted(sq("e8"), sq("g8")))
    }

    @Test
    fun blackQueenSideCastlingIsPermittedWithClearUnmovedPieces() {
        val game = customGame(customBoard("e8" to "BK4", "a8" to "BR0", "e1" to "WK4"), PieceColor.Black)
        assertTrue(game.castlingPermitted(sq("e8"), sq("c8")))
    }

    @Test
    fun doMoveExecutesBlackKingSideCastlingByMovingBothKingAndRook() {
        val game = customGame(customBoard("e8" to "BK4", "h8" to "BR7", "e1" to "WK4"), PieceColor.Black)
        val result = (game.doMove(sq("e8"), sq("g8")) as MoveResult.Success).game

        assertEquals(PieceType.King, result.board.pieceAt(sq("g8"))?.type)
        assertEquals(PieceType.Rook, result.board.pieceAt(sq("f8"))?.type)
        assertNull(result.board.pieceAt(sq("e8")))
        assertNull(result.board.pieceAt(sq("h8")))
    }

    @Test
    fun doMoveExecutesBlackQueenSideCastlingByMovingBothKingAndRook() {
        val game = customGame(customBoard("e8" to "BK4", "a8" to "BR0", "e1" to "WK4"), PieceColor.Black)
        val result = (game.doMove(sq("e8"), sq("c8")) as MoveResult.Success).game

        assertEquals(PieceType.King, result.board.pieceAt(sq("c8"))?.type)
        assertEquals(PieceType.Rook, result.board.pieceAt(sq("d8"))?.type)
        assertNull(result.board.pieceAt(sq("e8")))
        assertNull(result.board.pieceAt(sq("a8")))
    }

    // --- Bug: castlingPermitted checks the wrong side's threats when it isn't that piece's
    // actual turn to move ---

    @Test
    fun documentsBug_castlingPermittedChecksThreatsFromTheWrongColorWhenItIsNotThatSidesTurn() {
        // Same position as castlingThroughAnAttackedSquareIsBlocked: a black rook on f8
        // genuinely attacks f1 down the open file, so White castling kingside should be
        // illegal no matter whose turn it technically is right now.
        //
        // Why it isn't: castlingPermitted (Game.kt) computes `by = this.turn.other()` to
        // decide which color's attacks to check against the king's path - it infers the
        // attacking side from the GAME's current turn rather than from `piece.color.other()`
        // (the color of the king actually being asked to castle). canMove's King branch calls
        // castlingPermitted for whatever piece is at `from`, regardless of whose turn it is
        // (that's exactly how pieceIsThreatenedAt/positionIsThreatened/AI.search all probe
        // hypothetical moves) - so whenever this is evaluated on a not-your-turn position,
        // `by` resolves to the wrong color, and the check ends up asking "do White's own
        // pieces attack White's own castling path", which is never true.
        //
        // The history here is built directly (not via customGame) because a a1 bishop is
        // needed purely to anchor turn=Black without landing the turn-marker move on e1 or
        // h1, which would otherwise trip pieceHasMoved and mask this bug behind that check.
        val board = customBoard("e1" to "WK4", "h1" to "WR7", "f8" to "BR5", "g8" to "BK4", "a1" to "WB2")
        val game = Game(board = board, history = listOf(Move(sq("a1"), sq("a1"))))
        assertEquals(PieceColor.Black, game.turn)

        // Correct chess: still blocked, f1 is attacked by the f8 rook regardless of turn.
        // Buggy engine output: permitted, because `by` came out White instead of Black.
        assertTrue(game.castlingPermitted(sq("e1"), sq("g1")))
    }

    @Test
    @Ignore(
        "engine bug: castlingPermitted derives the attacking color from Game.turn instead of the moving " +
            "piece's color - see documentsBug_castlingPermittedChecksThreatsFromTheWrongColorWhenItIsNotThatSidesTurn",
    )
    fun spec_castlingThroughAnAttackedSquareIsBlockedRegardlessOfWhoseTurnItIs() {
        val board = customBoard("e1" to "WK4", "h1" to "WR7", "f8" to "BR5", "g8" to "BK4", "a1" to "WB2")
        val game = Game(board = board, history = listOf(Move(sq("a1"), sq("a1"))))

        assertFalse(game.castlingPermitted(sq("e1"), sq("g1")))
    }
}
