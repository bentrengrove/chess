package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `Board.fromHistory` always reconstructs from the standard starting `Board()` and replays
 * each `Move`. A FEN/PGN loader would lean on exactly this replay, so it needs to reproduce
 * castling (the rook has to jump too), en passant (the captured pawn has to disappear), and
 * promotion (the piece type has to change) - not just plain from/to slides.
 */
class BoardFromHistoryTest {
    // Plain-move replay correctness is covered by BoardTest.fromHistoryReplaysPlainMovesCorrectly
    // with concrete square assertions.

    @Test
    fun fromHistoryReplaysCastlingByMovingTheRookToo() {
        // Clear the king's path with real moves, then castle - exactly what a PGN's recorded
        // move list would contain: the king's two-square hop, nothing describing the rook.
        val history = listOf(Move(sq("g1"), sq("f3")), Move(sq("f1"), sq("c4")), Move(sq("e1"), sq("g1")))
        val replayed = Board.fromHistory(history)

        assertEquals(PieceType.King, replayed.pieceAt(sq("g1"))?.type)
        assertEquals(PieceType.Rook, replayed.pieceAt(sq("f1"))?.type)
        assertNull(replayed.pieceAt(sq("e1")))
        assertNull(replayed.pieceAt(sq("h1")))
    }

    @Test
    fun fromHistoryReplaysQueenSideCastlingByMovingTheRookToo() {
        val history = listOf(Move(sq("b1"), sq("c3")), Move(sq("c1"), sq("d2")), Move(sq("d1"), sq("b3")), Move(sq("e1"), sq("c1")))
        val replayed = Board.fromHistory(history)

        assertEquals(PieceType.King, replayed.pieceAt(sq("c1"))?.type)
        assertEquals(PieceType.Rook, replayed.pieceAt(sq("d1"))?.type)
        assertNull(replayed.pieceAt(sq("e1")))
        assertNull(replayed.pieceAt(sq("a1")))
    }

    @Test
    fun fromHistoryReplaysEnPassantByRemovingTheCapturedPawn() {
        val history =
            listOf(
                Move(sq("e2"), sq("e4")),
                Move(sq("g8"), sq("f6")),
                Move(sq("e4"), sq("e5")),
                Move(sq("d7"), sq("d5")),
                Move(sq("e5"), sq("d6")), // en passant capture of the pawn now sitting on d5
            )
        val replayed = Board.fromHistory(history)

        assertNull(replayed.pieceAt(sq("d5")))
        assertEquals(PieceType.Pawn, replayed.pieceAt(sq("d6"))?.type)
        assertEquals(PieceColor.White, replayed.pieceAt(sq("d6"))?.color)
    }

    @Test
    fun fromHistoryReplaysPromotionUsingTheRecordedChoice() {
        // fromHistory doesn't validate move legality (see GameCheckSafetyTest for the same
        // property on doMove), so a pawn can be walked straight up its file in the history
        // list - the point here is purely that the recorded promotion choice gets applied.
        val history =
            listOf(
                Move(sq("e2"), sq("e3")),
                Move(sq("e3"), sq("e4")),
                Move(sq("e4"), sq("e5")),
                Move(sq("e5"), sq("e6")),
                Move(sq("e6"), sq("e7")),
                Move(sq("e7"), sq("e8"), promotion = PieceType.Queen),
            )
        val replayed = Board.fromHistory(history)

        assertEquals(PieceType.Queen, replayed.pieceAt(sq("e8"))?.type)
        assertEquals(PieceColor.White, replayed.pieceAt(sq("e8"))?.color)
    }

    @Test
    fun fromHistoryReplaysPromotionToAnyChosenPieceType() {
        val history = listOf(Move(sq("e2"), sq("e7")), Move(sq("e7"), sq("e8"), promotion = PieceType.Knight))
        val replayed = Board.fromHistory(history)

        assertEquals(PieceType.Knight, replayed.pieceAt(sq("e8"))?.type)
    }

    // The following three drive a real Game via doMove (starting from the standard Game(),
    // since Board.fromHistory always assumes that starting point) and check that replaying
    // the resulting history reproduces the exact board doMove produced. That equality is the
    // actual property a FEN/PGN loader would depend on.

    @Test
    fun doMoveAndFromHistoryAgreeAfterCastling() {
        // The Italian Game's first few moves, ending in White castling kingside.
        val moves =
            listOf(
                sq("e2") to sq("e4"),
                sq("e7") to sq("e5"),
                sq("g1") to sq("f3"),
                sq("b8") to sq("c6"),
                sq("f1") to sq("c4"),
                sq("e1") to sq("g1"),
            )
        var game = Game()
        moves.forEach { (from, to) -> game = (game.doMove(from, to) as MoveResult.Success).game }

        assertEquals(PieceType.King, game.board.pieceAt(sq("g1"))?.type)
        assertEquals(PieceType.Rook, game.board.pieceAt(sq("f1"))?.type)
        assertEquals(game.board, Board.fromHistory(game.history))
    }

    @Test
    fun doMoveAndFromHistoryAgreeAfterEnPassant() {
        val moves =
            listOf(
                sq("e2") to sq("e4"),
                sq("g8") to sq("f6"),
                sq("e4") to sq("e5"),
                sq("d7") to sq("d5"),
                sq("e5") to sq("d6"), // en passant capture
            )
        var game = Game()
        moves.forEach { (from, to) -> game = (game.doMove(from, to) as MoveResult.Success).game }

        assertNull(game.board.pieceAt(sq("d5")))
        assertEquals(PieceType.Pawn, game.board.pieceAt(sq("d6"))?.type)
        assertEquals(game.board, Board.fromHistory(game.history))
    }

    @Test
    fun doMoveAndFromHistoryAgreeAfterPromotion() {
        // Not legal chess (a pawn can't capture straight ahead) - doMove doesn't validate a
        // move's shape (see GameCheckSafetyTest), so this just walks the a-pawn up its own
        // file to exercise promotion replay end-to-end through the real Game/doMove API
        // without needing a fully sound game to reach the back rank.
        val steps =
            listOf(
                sq("a2") to sq("a3"),
                sq("a3") to sq("a4"),
                sq("a4") to sq("a5"),
                sq("a5") to sq("a6"),
                sq("a6") to sq("a7"),
            )
        var game = Game()
        steps.forEach { (from, to) -> game = (game.doMove(from, to) as MoveResult.Success).game }
        val promotion = game.doMove(sq("a7"), sq("a8")) as MoveResult.Promotion
        game = (promotion.onPieceSelection(PieceType.Queen) as MoveResult.Success).game

        assertEquals(PieceType.Queen, game.board.pieceAt(sq("a8"))?.type)
        assertEquals(game.board, Board.fromHistory(game.history))
    }
}
