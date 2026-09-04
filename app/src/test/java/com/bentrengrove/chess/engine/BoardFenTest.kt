package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardFenTest {
    @Test
    fun standardStartingBoardExportsTheStandardFenPlacement() {
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR", Board().fen)
    }

    @Test
    fun standardFenPlacementParsesToExactlyTheStandardStartingBoard() {
        // Board.fromFen derives each piece's id from its color, type and file - the same
        // convention INITIAL_BOARD already uses ("BR0", "WN1", ...) - so a standard starting
        // FEN should reproduce a Board that's structurally identical to Board(), not just
        // one that looks the same piece-by-piece.
        assertEquals(Board(), Board.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"))
    }

    @Test
    fun fromFenPlacesPiecesOnASparsePositionAtTheCorrectSquares() {
        val board = Board.fromFen("4k3/8/8/8/4P3/8/8/4K3")

        assertEquals(PieceType.King, board.pieceAt(sq("e8"))?.type)
        assertEquals(PieceColor.Black, board.pieceAt(sq("e8"))?.color)
        assertEquals(PieceType.Pawn, board.pieceAt(sq("e4"))?.type)
        assertEquals(PieceColor.White, board.pieceAt(sq("e4"))?.color)
        assertEquals(PieceType.King, board.pieceAt(sq("e1"))?.type)
        assertEquals(PieceColor.White, board.pieceAt(sq("e1"))?.color)
        assertNull(board.pieceAt(sq("a1")))
        assertNull(board.pieceAt(sq("d4")))
    }

    @Test
    fun fenExportRoundTripsThroughFromFenForASparsePosition() {
        val fen = "4k3/8/2p5/8/4P3/8/8/2B1K3"
        assertEquals(fen, Board.fromFen(fen).fen)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromFenRejectsFewerThanEightRanks() {
        Board.fromFen("8/8/8/8/8/8/8")
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromFenRejectsARankThatDoesNotDescribeEightSquares() {
        Board.fromFen("4k3/8/8/8/8/8/8/3K3")
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromFenRejectsAnUnknownPieceLetter() {
        Board.fromFen("4k3/8/8/8/8/8/8/4XK2")
    }
}
