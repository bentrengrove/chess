package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GamePromotionTest {
    @Test
    fun whitePawnReachingTheEighthRankTriggersPromotion() {
        val board = customBoard("e7" to "WP0", "e1" to "WK4", "a8" to "BK4")
        val game = customGame(board, PieceColor.White)

        val result = game.doMove(sq("e7"), sq("e8"))

        assertTrue(result is MoveResult.Promotion)
        assertEquals(PieceColor.White, (result as MoveResult.Promotion).color)
    }

    @Test
    fun blackPawnReachingTheFirstRankTriggersPromotion() {
        val board = customBoard("e2" to "BP0", "e8" to "BK4", "a1" to "WK4")
        val game = customGame(board, PieceColor.Black)

        val result = game.doMove(sq("e2"), sq("e1"))

        assertTrue(result is MoveResult.Promotion)
        assertEquals(PieceColor.Black, (result as MoveResult.Promotion).color)
    }

    @Test
    fun selectingAPromotionPieceReplacesThePawnOnTheBoard() {
        val board = customBoard("e7" to "WP0", "e1" to "WK4", "a8" to "BK4")
        val game = customGame(board, PieceColor.White)

        val promotion = game.doMove(sq("e7"), sq("e8")) as MoveResult.Promotion
        val result = promotion.onPieceSelection(PieceType.Queen)

        assertTrue(result is MoveResult.Success)
        val finalPiece = (result as MoveResult.Success).game.board.pieceAt(sq("e8"))
        assertEquals(PieceType.Queen, finalPiece?.type)
        assertEquals(PieceColor.White, finalPiece?.color)
    }

    @Test
    fun promotionCanResolveToAnyOfTheFourPieceTypes() {
        val board = customBoard("e7" to "WP0", "e1" to "WK4", "a8" to "BK4")
        val game = customGame(board, PieceColor.White)
        val promotion = game.doMove(sq("e7"), sq("e8")) as MoveResult.Promotion

        listOf(PieceType.Knight, PieceType.Bishop, PieceType.Rook, PieceType.Queen).forEach { pieceType ->
            val result = promotion.onPieceSelection(pieceType) as MoveResult.Success
            val finalPiece = result.game.board.pieceAt(sq("e8"))
            assertEquals(pieceType, finalPiece?.type)
        }
    }

    @Test
    fun pawnNotOnTheBackRankDoesNotTriggerPromotion() {
        val result = Game().doMove(sq("e2"), sq("e3"))
        assertTrue(result is MoveResult.Success)
    }

    @Test
    fun canPromotePieceAtIsFalseForNonPawnPiecesOnTheBackRank() {
        val board = customBoard("e8" to "WQ3", "e1" to "WK4", "a8" to "BK4")
        val game = customGame(board, PieceColor.White)
        assertEquals(false, game.canPromotePieceAt(sq("e8")))
    }
}
