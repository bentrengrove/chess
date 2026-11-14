package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class GameFenCapturedPiecesTest {
    // 1. e4 Nf6 2. Nf3: both knights have left their home files, but nothing is captured.
    private val knightsOutFen = "rnbqkb1r/pppppppp/5n2/8/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"

    private fun Game.capturedTypes(color: PieceColor) = capturedPiecesFor(color).map { it.type }

    private fun Board.ids() = allPieces.map { it.second.id }

    @Test
    fun aFenWithPiecesOffTheirHomeFilesReportsNoCapturedPieces() {
        val game = Game.fromFen(knightsOutFen)

        assertEquals(emptyList<Piece>(), game.capturedPiecesFor(PieceColor.White))
        assertEquals(emptyList<Piece>(), game.capturedPiecesFor(PieceColor.Black))
    }

    @Test
    fun aFenWithACastledKingReportsNoCapturedPieces() {
        // Italian Four Knights with both sides castled kingside: every minor piece and king is off its home file.
        val game = Game.fromFen("r1bq1rk1/pppp1ppp/2n2n2/2b1p3/2B1P3/2N2N2/PPPP1PPP/R1BQ1RK1 w - - 6 6")

        assertEquals(emptyList<Piece>(), game.capturedPiecesFor(PieceColor.White))
        assertEquals(emptyList<Piece>(), game.capturedPiecesFor(PieceColor.Black))
    }

    @Test
    fun aFenWithDoubledPawnsAndRooksHasUniquePieceIds() {
        // White has doubled c-pawns and doubled rooks on the d-file; Black has tripled f-pawns.
        val board = Board.fromFen("4k3/5p2/5p2/5p2/8/2P5/2PR4/3RK3")

        val ids = board.ids()
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun theStandardFenKeepsTheStandardIds() {
        assertEquals(Board().ids(), Board.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR").ids())
    }

    @Test
    fun aFenWithMissingMaterialReportsItAsCapturedByType() {
        // White is missing the d-pawn and a knight; Black is missing the queen.
        val game = Game.fromFen("rnb1kbnr/pppppppp/8/8/8/8/PPP1PPPP/R1BQKBNR w KQkq - 0 1")

        assertEquals(listOf(PieceType.Pawn, PieceType.Knight), game.capturedTypes(PieceColor.White))
        assertEquals(listOf(PieceType.Queen), game.capturedTypes(PieceColor.Black))
    }

    @Test
    fun anExtraPromotedPieceInAFenDoesNotCountAsNegativeCaptures() {
        // White has two queens and seven pawns: the extra queen is clamped, the pawn is missing.
        val game = Game.fromFen("4k3/8/8/8/8/8/PPPPPPP1/RNBQKBNQ w - - 0 1")

        assertEquals(listOf(PieceType.Pawn, PieceType.Rook), game.capturedTypes(PieceColor.White))
    }

    @Test
    fun aCaptureAfterLoadingAFenIsReportedAndUndoneCleanly() {
        // 1. e4 d5 loaded, then exd5 captures a pawn.
        val start = Game.fromFen("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2")
        val afterCapture = start.play(Move(sq("e4"), sq("d5")))

        assertEquals(listOf(PieceType.Pawn), afterCapture.capturedTypes(PieceColor.Black))
        assertEquals(emptyList<Piece>(), afterCapture.capturedPiecesFor(PieceColor.White))

        val undone = afterCapture.undoLastMove()
        assertEquals(start.fen, undone.fen)
        assertEquals(start.board, undone.board)
        assertEquals(emptyList<Piece>(), undone.capturedPiecesFor(PieceColor.Black))

        val redone = undone.play(afterCapture.history.last())
        assertEquals(afterCapture.board, redone.board)
        assertEquals(listOf(PieceType.Pawn), redone.capturedTypes(PieceColor.Black))
    }

    @Test
    fun undoAndRedoKeepIdsUniqueAndStableForADoubledPawnFen() {
        val start = Game.fromFen("4k3/8/8/8/8/2P5/2P5/4K3 w - - 0 1")
        val played = start.play(Move(sq("c3"), sq("c4"))).play(Move(sq("e8"), sq("d7")))

        val ids = played.board.ids()
        assertEquals(ids.size, ids.toSet().size)

        val undone = played.undoLastMove().undoLastMove()
        assertEquals(start.board, undone.board)

        val redone = undone.replay(played.history)
        assertEquals(played.board, redone.board)
    }

    @Test
    fun aStandardGameStillReportsCapturesById() {
        val game = Game.fromPgn("1. e4 d5 2. exd5 Qxd5")

        assertEquals(listOf(PieceType.Pawn), game.capturedTypes(PieceColor.White))
        assertEquals(listOf(PieceType.Pawn), game.capturedTypes(PieceColor.Black))
        assertEquals(listOf("WP4"), game.capturedPiecesFor(PieceColor.White).map { it.id })
        assertEquals(listOf("BP3"), game.capturedPiecesFor(PieceColor.Black).map { it.id })
    }
}
