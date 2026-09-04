package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameMovementRulesTest {
    private fun gameOn(board: Board) = customGame(board, PieceColor.White)

    // --- Pawns ---

    @Test
    fun whitePawnCanAdvanceOneOrTwoSquaresFromStartingRank() {
        val game = Game()
        assertTrue(game.canMove(sq("e2"), sq("e3")))
        assertTrue(game.canMove(sq("e2"), sq("e4")))
    }

    @Test
    fun whitePawnCannotAdvanceThreeSquares() {
        val game = Game()
        assertFalse(game.canMove(sq("e2"), sq("e5")))
    }

    @Test
    fun whitePawnNotOnStartingRankCanOnlyMoveOneSquare() {
        val game = customGame(customBoard("e3" to "WP0", "e1" to "WK4", "e8" to "BK4"), PieceColor.White)
        assertTrue(game.canMove(sq("e3"), sq("e4")))
        assertFalse(game.canMove(sq("e3"), sq("e5")))
    }

    @Test
    fun whitePawnDoubleStepIsBlockedByAnInterveningPiece() {
        val game = customGame(customBoard("e2" to "WP0", "e3" to "BP0", "e1" to "WK4", "e8" to "BK4"), PieceColor.White)
        assertFalse(game.canMove(sq("e2"), sq("e4")))
    }

    @Test
    fun pawnCannotMoveStraightIntoAnOccupiedSquare() {
        val game = customGame(customBoard("e4" to "WP0", "e5" to "BP0", "e1" to "WK4", "e8" to "BK4"), PieceColor.White)
        assertFalse(game.canMove(sq("e4"), sq("e5")))
    }

    @Test
    fun pawnCanCaptureDiagonallyOntoAnEnemyPiece() {
        val game = customGame(customBoard("e4" to "WP0", "d5" to "BP0", "e1" to "WK4", "e8" to "BK4"), PieceColor.White)
        assertTrue(game.canMove(sq("e4"), sq("d5")))
    }

    @Test
    fun pawnCannotCaptureDiagonallyOntoItsOwnPiece() {
        val game = customGame(customBoard("e4" to "WP0", "d5" to "WP1", "e1" to "WK4", "e8" to "BK4"), PieceColor.White)
        assertFalse(game.canMove(sq("e4"), sq("d5")))
    }

    @Test
    fun pawnCannotCaptureDiagonallyIntoAnEmptySquare() {
        val game = customGame(customBoard("e4" to "WP0", "e1" to "WK4", "e8" to "BK4"), PieceColor.White)
        assertFalse(game.canMove(sq("e4"), sq("d5")))
    }

    @Test
    fun blackPawnAdvancesDownTheBoard() {
        val game = customGame(customBoard("e7" to "BP0", "e1" to "WK4", "e8" to "BK4"), PieceColor.Black)
        assertTrue(game.canMove(sq("e7"), sq("e6")))
        assertTrue(game.canMove(sq("e7"), sq("e5")))
        assertFalse(game.canMove(sq("e7"), sq("e4")))
    }

    // --- Rooks ---

    @Test
    fun rookMovesAnyDistanceHorizontallyOrVertically() {
        val game = gameOn(customBoard("d4" to "WR0", "e1" to "WK4", "e8" to "BK4"))
        assertTrue(game.canMove(sq("d4"), sq("d8")))
        assertTrue(game.canMove(sq("d4"), sq("a4")))
    }

    @Test
    fun rookCannotMoveDiagonally() {
        val game = gameOn(customBoard("d4" to "WR0", "e1" to "WK4", "e8" to "BK4"))
        assertFalse(game.canMove(sq("d4"), sq("f6")))
    }

    @Test
    fun rookIsBlockedByAPieceInItsPath() {
        val game = gameOn(customBoard("d4" to "WR0", "d6" to "WP0", "e1" to "WK4", "e8" to "BK4"))
        assertFalse(game.canMove(sq("d4"), sq("d8")))
    }

    @Test
    fun rookCanCaptureAnEnemyAtTheEndOfItsPath() {
        val game = gameOn(customBoard("d4" to "WR0", "d8" to "BP0", "e1" to "WK4", "e8" to "BK4"))
        assertTrue(game.canMove(sq("d4"), sq("d8")))
    }

    // --- Bishops ---

    @Test
    fun bishopMovesAnyDistanceDiagonally() {
        val game = gameOn(customBoard("c1" to "WB2", "e1" to "WK4", "e8" to "BK4"))
        assertTrue(game.canMove(sq("c1"), sq("h6")))
    }

    @Test
    fun bishopCannotMoveHorizontallyOrVertically() {
        val game = gameOn(customBoard("d4" to "WB2", "e1" to "WK4", "e8" to "BK4"))
        assertFalse(game.canMove(sq("d4"), sq("d8")))
        assertFalse(game.canMove(sq("d4"), sq("a4")))
    }

    @Test
    fun bishopIsBlockedByAPieceInItsPath() {
        val game = gameOn(customBoard("c1" to "WB2", "e3" to "WP0", "e1" to "WK4", "e8" to "BK4"))
        assertFalse(game.canMove(sq("c1"), sq("h6")))
    }

    // --- Queen ---

    @Test
    fun queenCombinesRookAndBishopMovement() {
        val game = gameOn(customBoard("d4" to "WQ3", "e1" to "WK4", "e8" to "BK4"))
        assertTrue(game.canMove(sq("d4"), sq("d8")))
        assertTrue(game.canMove(sq("d4"), sq("a4")))
        assertTrue(game.canMove(sq("d4"), sq("a7")))
    }

    @Test
    fun queenCannotMoveLikeAKnight() {
        val game = gameOn(customBoard("d4" to "WQ3", "e1" to "WK4", "e8" to "BK4"))
        assertFalse(game.canMove(sq("d4"), sq("f5")))
    }

    // --- Knight ---

    @Test
    fun knightMovesInAnLShapeIgnoringBlockingPieces() {
        val game =
            gameOn(
                customBoard(
                    "d4" to "WN0",
                    "d5" to "WP0",
                    "d6" to "WP1",
                    "e1" to "WK4",
                    "e8" to "BK4",
                ),
            )
        assertTrue(game.canMove(sq("d4"), sq("e6")))
        assertTrue(game.canMove(sq("d4"), sq("f5")))
        assertTrue(game.canMove(sq("d4"), sq("c2")))
    }

    @Test
    fun knightCannotMoveOutsideItsLShapes() {
        val game = gameOn(customBoard("d4" to "WN0", "e1" to "WK4", "e8" to "BK4"))
        assertFalse(game.canMove(sq("d4"), sq("d6")))
        assertFalse(game.canMove(sq("d4"), sq("e5")))
    }

    // --- King ---

    @Test
    fun kingMovesOneSquareInAnyDirection() {
        val game = gameOn(customBoard("d4" to "WK4", "e8" to "BK4"))
        assertTrue(game.canMove(sq("d4"), sq("d5")))
        assertTrue(game.canMove(sq("d4"), sq("e5")))
        assertTrue(game.canMove(sq("d4"), sq("c3")))
    }

    @Test
    fun kingCannotMoveTwoSquaresOutsideOfCastling() {
        val game = gameOn(customBoard("d4" to "WK4", "e8" to "BK4"))
        assertFalse(game.canMove(sq("d4"), sq("d6")))
    }

    // --- Cross-cutting ---

    @Test
    fun canMoveRejectsCapturingYourOwnPiece() {
        val game = gameOn(customBoard("d4" to "WR0", "d6" to "WP0", "e1" to "WK4", "e8" to "BK4"))
        assertFalse(game.canMove(sq("d4"), sq("d6")))
    }

    @Test
    fun canMoveFromAnEmptySquareIsFalse() {
        val game = Game()
        assertFalse(game.canMove(sq("e4"), sq("e5")))
    }

    @Test
    fun movesForPieceAtListsEveryPseudoLegalDestination() {
        val game = Game()
        val moves = game.movesForPieceAt(sq("b1")).toSet()
        assertEquals(setOf(sq("a3"), sq("c3")), moves)
    }

    @Test
    fun movesForPieceAtNullPositionIsEmpty() {
        assertTrue(Game().movesForPieceAt(null).isEmpty())
    }

    @Test
    fun startingPositionHasTwentyLegalMovesForEachSide() {
        val game = Game()
        assertEquals(20, game.allMovesFor(PieceColor.White).toList().size)
        assertEquals(20, game.allMovesFor(PieceColor.Black).toList().size)
    }

    @Test
    fun canSelectOnlyAllowsPickingUpThePieceWhoseTurnItIs() {
        val game = Game()
        assertTrue(game.canSelect(sq("e2")))
        assertFalse(game.canSelect(sq("e7")))
    }
}
