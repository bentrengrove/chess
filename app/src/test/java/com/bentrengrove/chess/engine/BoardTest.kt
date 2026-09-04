package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTest {
    @Test
    fun startingBoardHasThirtyTwoPieces() {
        assertEquals(32, Board().allPieces.size)
    }

    @Test
    fun startingBoardHasSixtyFourSquares() {
        assertEquals(64, Board().allPositions.size)
    }

    @Test
    fun startingPositionPlacesWhiteBackRankCorrectly() {
        val board = Board()
        assertEquals(PieceType.Rook, board.pieceAt(sq("a1"))?.type)
        assertEquals(PieceType.Knight, board.pieceAt(sq("b1"))?.type)
        assertEquals(PieceType.Bishop, board.pieceAt(sq("c1"))?.type)
        assertEquals(PieceType.Queen, board.pieceAt(sq("d1"))?.type)
        assertEquals(PieceType.King, board.pieceAt(sq("e1"))?.type)
        board.allPieces.filter { it.first.y == 7 }.forEach {
            assertEquals(PieceColor.White, it.second.color)
        }
    }

    @Test
    fun startingPositionPlacesBlackBackRankCorrectly() {
        val board = Board()
        assertEquals(PieceType.Rook, board.pieceAt(sq("a8"))?.type)
        assertEquals(PieceType.King, board.pieceAt(sq("e8"))?.type)
        board.allPieces.filter { it.first.y == 0 }.forEach {
            assertEquals(PieceColor.Black, it.second.color)
        }
    }

    @Test
    fun middleRanksAreEmptyAtStart() {
        val board = Board()
        (2..5).forEach { y ->
            (0..7).forEach { x ->
                assertNull(board.pieceAt(Position(x, y)))
            }
        }
    }

    @Test
    fun pieceAtOutOfBoundsReturnsNullRatherThanThrowing() {
        val board = Board()
        assertNull(board.pieceAt(Position(-1, 0)))
        assertNull(board.pieceAt(Position(0, -1)))
        assertNull(board.pieceAt(Position(8, 0)))
        assertNull(board.pieceAt(Position(0, 8)))
    }

    @Test
    fun movePieceRelocatesPieceAndClearsOrigin() {
        val board = Board()
        val moved = board.movePiece(sq("e2"), sq("e4"))
        assertNull(moved.pieceAt(sq("e2")))
        assertEquals(PieceType.Pawn, moved.pieceAt(sq("e4"))?.type)
        assertEquals(PieceColor.White, moved.pieceAt(sq("e4"))?.color)
    }

    @Test
    fun movePieceDoesNotMutateOriginalBoard() {
        val board = Board()
        board.movePiece(sq("e2"), sq("e4"))
        assertEquals(PieceType.Pawn, board.pieceAt(sq("e2"))?.type)
        assertNull(board.pieceAt(sq("e4")))
    }

    @Test
    fun movePieceOntoOccupiedSquareCapturesInPlace() {
        val board = customBoard("e4" to "WP0", "d5" to "BP0")
        val moved = board.movePiece(sq("e4"), sq("d5"))
        assertEquals(PieceColor.White, moved.pieceAt(sq("d5"))?.color)
        assertNull(moved.pieceAt(sq("e4")))
        assertEquals(1, moved.allPieces.size)
    }

    @Test
    fun promotePieceChangesTypeButKeepsColorAndSquare() {
        val board = customBoard("e8" to "WP0")
        val promoted = board.promotePiece(sq("e8"), PieceType.Queen)
        val piece = promoted.pieceAt(sq("e8"))
        assertEquals(PieceType.Queen, piece?.type)
        assertEquals(PieceColor.White, piece?.color)
    }

    @Test
    fun promotePieceAtEmptySquareIsANoOp() {
        val board = Board()
        val result = board.promotePiece(sq("e4"), PieceType.Queen)
        assertEquals(board, result)
    }

    @Test
    fun removePieceClearsSquare() {
        val board = Board()
        val result = board.removePiece(sq("e2"))
        assertNull(result.pieceAt(sq("e2")))
        assertEquals(31, result.allPieces.size)
    }

    @Test
    fun removePieceAtEmptySquareIsANoOp() {
        val board = Board()
        val result = board.removePiece(sq("e4"))
        assertEquals(board, result)
    }

    @Test
    fun firstPositionFindsMatchingPiece() {
        val board = Board()
        val position = board.firstPosition { it.type is PieceType.King && it.color == PieceColor.White }
        assertEquals(sq("e1"), position)
    }

    @Test
    fun firstPositionReturnsNullWhenNoneMatch() {
        val board = customBoard("e1" to "WK4")
        val position = board.firstPosition { it.type is PieceType.Queen }
        assertNull(position)
    }

    @Test
    fun fromHistoryReplaysPlainMovesCorrectly() {
        val history = listOf(Move(sq("e2"), sq("e4")), Move(sq("e7"), sq("e5")), Move(sq("g1"), sq("f3")))
        val board = Board.fromHistory(history)

        assertNull(board.pieceAt(sq("e2")))
        assertEquals(PieceType.Pawn, board.pieceAt(sq("e4"))?.type)
        assertNull(board.pieceAt(sq("e7")))
        assertEquals(PieceType.Pawn, board.pieceAt(sq("e5"))?.type)
        assertNull(board.pieceAt(sq("g1")))
        assertEquals(PieceType.Knight, board.pieceAt(sq("f3"))?.type)
        assertEquals(32, board.allPieces.size)
    }

    @Test
    fun allPiecesOnlyContainsOccupiedSquares() {
        val board = customBoard("e1" to "WK4", "e8" to "BK4")
        assertEquals(2, board.allPieces.size)
        assertTrue(board.allPieces.any { it.first == sq("e1") })
        assertTrue(board.allPieces.any { it.first == sq("e8") })
    }
}
