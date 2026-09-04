package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PieceTest {
    @Test
    fun parsesColorAndTypeFromId() {
        val piece = Piece.pieceFromString("WQ3")
        assertEquals(PieceColor.White, piece.color)
        assertEquals(PieceType.Queen, piece.type)
        assertEquals("WQ3", piece.id)
    }

    @Test
    fun parsesBlackPiece() {
        val piece = Piece.pieceFromString("BN1")
        assertEquals(PieceColor.Black, piece.color)
        assertEquals(PieceType.Knight, piece.type)
    }

    @Test
    fun pieceOrNullFromStringReturnsNullForNullInput() {
        assertNull(Piece.pieceOrNullFromString(null))
    }

    @Test
    fun pieceOrNullFromStringParsesNonNullInput() {
        val piece = Piece.pieceOrNullFromString("BK4")
        assertEquals(PieceType.King, piece?.type)
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsIdsOfWrongLength() {
        Piece.pieceFromString("WQ")
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsUnknownColorCharacter() {
        Piece.pieceFromString("XQ3")
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsUnknownTypeCharacter() {
        Piece.pieceFromString("WX3")
    }

    @Test
    fun colorOtherFlipsBetweenWhiteAndBlack() {
        assertEquals(PieceColor.Black, PieceColor.White.other())
        assertEquals(PieceColor.White, PieceColor.Black.other())
    }

    @Test
    fun pieceEqualityIsStructural() {
        assertEquals(Piece.pieceFromString("WP0"), Piece.pieceFromString("WP0"))
        assertTrue(Piece.pieceFromString("WP0") != Piece.pieceFromString("WP1"))
    }

    @Test
    fun allPieceTypesHaveExpectedStandardValues() {
        assertEquals(1, PieceType.Pawn.value)
        assertEquals(3, PieceType.Knight.value)
        assertEquals(3, PieceType.Bishop.value)
        assertEquals(5, PieceType.Rook.value)
        assertEquals(8, PieceType.Queen.value)
        assertEquals(0, PieceType.King.value)
    }
}
