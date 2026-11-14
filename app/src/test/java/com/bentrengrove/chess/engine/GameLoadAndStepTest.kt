package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameLoadAndStepTest {
    @Test
    fun fromFenOrPgnLoadsAFenString() {
        val fen = "4k3/8/8/8/8/8/4P3/4K3 b - - 3 42"
        val game = Game.fromFenOrPgn("  $fen\n")

        assertEquals(fen, game.fen)
        assertEquals(PieceColor.Black, game.turn)
    }

    @Test
    fun fromFenOrPgnLoadsPgnMovetext() {
        val game = Game.fromFenOrPgn("1. e4 e5 2. Nf3 Nc6")

        assertEquals(4, game.history.size)
        assertEquals(PieceType.Knight, game.board.pieceAt(sq("c6"))?.type)
    }

    @Test
    fun fromFenOrPgnLoadsPgnWithTagsIncludingAFenTag() {
        val game = Game.fromFenOrPgn("[Event \"Test\"]\n[FEN \"4k3/8/8/8/8/8/4P3/4K3 w - - 0 1\"]\n\n1. e4 Kd7 *")

        assertEquals(2, game.history.size)
        assertEquals(PieceType.Pawn, game.board.pieceAt(sq("e4"))?.type)
        assertEquals(PieceType.King, game.board.pieceAt(sq("d7"))?.type)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromFenOrPgnRejectsGarbage() {
        Game.fromFenOrPgn("not a chess game")
    }

    @Test
    fun undoLastMoveFromAStandardGameRestoresTheEarlierPosition() {
        val game = Game.fromPgn("1. e4 e5 2. Nf3")

        val undone = game.undoLastMove()

        assertEquals(Game.fromPgn("1. e4 e5").fen, undone.fen)
        assertEquals(2, undone.history.size)
    }

    @Test
    fun undoLastMoveFromAFenGameRebuildsFromTheFenPositionNotTheStandardStart() {
        // Board.fromHistory always replays from the standard start, so stepping back in a game
        // loaded from FEN used to show the standard starting pieces.
        val start = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"
        val game = Game.fromPgn("[FEN \"$start\"]\n\n1. e4 Kd7")

        val once = game.undoLastMove()
        assertEquals(PieceType.King, once.board.pieceAt(sq("e8"))?.type)
        assertEquals(PieceType.Pawn, once.board.pieceAt(sq("e4"))?.type)
        assertEquals(PieceColor.Black, once.turn)

        val twice = once.undoLastMove()
        assertEquals(start, twice.fen)
        assertNull(twice.board.pieceAt(sq("a1")))
    }

    @Test
    fun undoLastMoveFromAFenGameWithBlackToMoveKeepsTheClocks() {
        val start = "4k3/8/8/8/8/8/4P3/4K3 b - - 7 30"
        val game = Game.fromPgn("[FEN \"$start\"]\n\n30... Kd7 31. Kd2")

        assertEquals(start, game.undoLastMove().undoLastMove().fen)
    }

    @Test
    fun steppingBackThenForwardReturnsToTheSamePosition() {
        val game = Game.fromPgn("1. e4 d5 2. exd5 Qxd5 3. Nc3")
        val lastMove = game.history.last()

        val redone = game.undoLastMove().play(lastMove)

        assertEquals(game.fen, redone.fen)
        assertEquals(game.history, redone.history)
    }

    @Test
    fun playReplaysARecordedUnderPromotionWithoutAskingAgain() {
        val game = Game.fromPgn("[FEN \"4k3/7P/8/8/8/8/8/4K3 w - - 0 1\"]\n\n1. h8=N")
        val promotion = game.history.last()

        val redone = game.undoLastMove().play(promotion)

        assertEquals(PieceType.Knight, redone.board.pieceAt(sq("h8"))?.type)
    }

    @Test
    fun startingFullmoveNumberForAStandardGameIsOne() {
        assertEquals(1, Game.fromPgn("1. e4 e5 2. Nf3").startingFullmoveNumber)
    }

    @Test
    fun startingFullmoveNumberComesFromTheFen() {
        val whiteFirst = Game.fromPgn("[FEN \"4k3/8/8/8/8/8/4P3/4K3 w - - 0 12\"]\n\n12. e4 Kd7 13. Kd2")
        assertEquals(12, whiteFirst.startingFullmoveNumber)

        val blackFirst = Game.fromPgn("[FEN \"4k3/8/8/8/8/8/4P3/4K3 b - - 0 12\"]\n\n12... Kd7 13. e4")
        assertEquals(12, blackFirst.startingFullmoveNumber)
    }
}
