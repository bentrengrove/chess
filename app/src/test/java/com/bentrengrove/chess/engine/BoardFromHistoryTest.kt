package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `Board.fromHistory` always reconstructs from the standard starting `Board()` and replays
 * each `Move` with a bare `movePiece` (Board.kt) - no rook relocation for castling, no
 * en-passant pawn removal, no promotion. A FEN/PGN loader would lean on exactly this replay,
 * so these pin down the gap now: build the "as replayed" board and the "as it should be" board
 * side by side, both by direct [Board] mutation (not [Game.doMove]) so the comparison isn't
 * entangled with whether every intermediate move was chess-legal - only the replay mechanics
 * are in scope here.
 */
class BoardFromHistoryTest {
    // Plain-move replay correctness is covered by BoardTest.fromHistoryReplaysPlainMovesCorrectly
    // with concrete square assertions.

    @Test
    fun documentsBug_fromHistoryDoesNotRelocateTheRookOnCastling() {
        // Clear the king's path with real moves, then "castle" by moving just the king two
        // squares - exactly what a PGN's recorded move list would contain.
        val history = listOf(Move(sq("g1"), sq("f3")), Move(sq("f1"), sq("c4")), Move(sq("e1"), sq("g1")))
        val replayed = Board.fromHistory(history)

        // Bug: fromHistory only ever moves the piece named in each Move, so the king ends up
        // on g1 but the rook is left behind on h1 instead of jumping to f1.
        assertNull(replayed.pieceAt(sq("f1")))
        assertEquals(PieceType.Rook, replayed.pieceAt(sq("h1"))?.type)

        // What real castling would have produced: king to g1 *and* rook h1 to f1.
        var correct = Board()
        history.dropLast(1).forEach { correct = correct.movePiece(it.from, it.to) }
        correct = correct.movePiece(sq("h1"), sq("f1")).movePiece(sq("e1"), sq("g1"))

        assertEquals(PieceType.Rook, correct.pieceAt(sq("f1"))?.type)
        assertNotEquals(correct, replayed)
    }

    @Test
    fun documentsBug_fromHistoryDoesNotRemoveTheCapturedEnPassantPawn() {
        val history =
            listOf(
                Move(sq("e2"), sq("e4")),
                Move(sq("g8"), sq("f6")),
                Move(sq("e4"), sq("e5")),
                Move(sq("d7"), sq("d5")),
                Move(sq("e5"), sq("d6")), // en passant capture of the pawn now sitting on d5
            )
        val replayed = Board.fromHistory(history)

        // Bug: the black pawn that was actually captured en passant on d5 is still there,
        // because fromHistory never removes anything except by landing directly on top of it.
        assertEquals(PieceType.Pawn, replayed.pieceAt(sq("d5"))?.type)
        assertEquals(PieceColor.Black, replayed.pieceAt(sq("d5"))?.color)

        // What a real en-passant replay would have produced: d5 empty, white pawn on d6.
        var correct = Board()
        history.dropLast(1).forEach { correct = correct.movePiece(it.from, it.to) }
        correct = correct.removePiece(sq("d5")).movePiece(sq("e5"), sq("d6"))

        assertNull(correct.pieceAt(sq("d5")))
        assertNotEquals(correct, replayed)
    }

    @Test
    fun documentsBug_fromHistoryDoesNotApplyPromotion() {
        // fromHistory doesn't validate move legality either, so a pawn can be walked straight
        // up its file in the history list - the point here is purely the missing promotion.
        val history =
            listOf(
                Move(sq("e2"), sq("e3")),
                Move(sq("e3"), sq("e4")),
                Move(sq("e4"), sq("e5")),
                Move(sq("e5"), sq("e6")),
                Move(sq("e6"), sq("e7")),
                Move(sq("e7"), sq("e8")),
            )
        val replayed = Board.fromHistory(history)

        // Bug: fromHistory has no concept of the promotion choice, so the piece on e8 is still
        // the pawn that walked there instead of the queen it should have promoted into.
        assertEquals(PieceType.Pawn, replayed.pieceAt(sq("e8"))?.type)

        var correct = Board()
        history.forEach { correct = correct.movePiece(it.from, it.to) }
        correct = correct.promotePiece(sq("e8"), PieceType.Queen)

        assertEquals(PieceType.Queen, correct.pieceAt(sq("e8"))?.type)
        assertNotEquals(correct, replayed)
    }
}
