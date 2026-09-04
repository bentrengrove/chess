package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameFenTest {
    private val startingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    @Test
    fun freshGameExportsTheStandardStartingFen() {
        assertEquals(startingFen, Game().fen)
    }

    @Test
    fun theStandardStartingFenParsesBackToAFreshGame() {
        val game = Game.fromFen(startingFen)

        assertEquals(PieceColor.White, game.turn)
        assertEquals(Board(), game.board)
        assertEquals(startingFen, game.fen)
    }

    @Test
    fun fromFenHonorsTheActiveColorField() {
        val game = Game.fromFen("4k3/8/8/8/8/8/8/4K3 b - - 0 1")
        assertEquals(PieceColor.Black, game.turn)
    }

    @Test
    fun fromFenHonorsRestrictedCastlingRights() {
        // Kingside-only for White, nothing for Black: the position is otherwise a normal
        // castling-ready setup, so only the omitted rights should be blocked.
        val game = Game.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w K - 0 1")

        assertTrue(game.castlingPermitted(sq("e1"), sq("g1")))
        assertFalse(game.castlingPermitted(sq("e1"), sq("c1")))
        assertFalse(game.castlingPermitted(sq("e8"), sq("g8")))
        assertFalse(game.castlingPermitted(sq("e8"), sq("c8")))
    }

    @Test
    fun fromFenWithNoCastlingRightsBlocksCastlingEvenWithUnmovedRooksAndKing() {
        val game = Game.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1")
        assertFalse(game.castlingPermitted(sq("e1"), sq("g1")))
        assertFalse(game.castlingPermitted(sq("e1"), sq("c1")))
    }

    @Test
    fun currentCastlingRightsCanBeFurtherRestrictedByMovesAfterLoading() {
        val game = Game.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        val afterRookMove = (game.doMove(sq("h1"), sq("h4")) as MoveResult.Success).game

        assertFalse(afterRookMove.castlingPermitted(sq("e1"), sq("g1")))
        // Queenside for White and both sides for Black are untouched by that one rook move.
        assertTrue(afterRookMove.castlingPermitted(sq("e8"), sq("g8")))
    }

    @Test
    fun fromFenHonorsAnExplicitEnPassantTarget() {
        // As if Black had just played d7-d5: White to move, e5 pawn can take en passant on d6.
        val game = Game.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1")

        assertTrue(game.enPassantTakePermitted(sq("e5"), sq("d6")))
        val result = (game.doMove(sq("e5"), sq("d6")) as MoveResult.Success).game
        assertEquals(null, result.board.pieceAt(sq("d5")))
    }

    @Test
    fun enPassantTargetDoesNotSurviveARealMoveOfTheGame() {
        val game = Game.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1")
        // A move that isn't the en passant capture itself should clear the stale target.
        val afterOtherMove = (game.doMove(sq("e1"), sq("e2")) as MoveResult.Success).game

        assertFalse(afterOtherMove.enPassantTakePermitted(sq("e5"), sq("d6")))
        assertEquals(null, afterOtherMove.currentEnPassantTarget)
    }

    @Test
    fun fenExportReflectsTheCurrentEnPassantTargetAfterADoubleStep() {
        val game = (Game().doMove(sq("e2"), sq("e4")) as MoveResult.Success).game
        assertEquals("e3", game.currentEnPassantTarget?.toAlgebraic())
        assertTrue(game.fen.contains(" e3 "))
    }

    @Test
    fun halfmoveClockIncrementsOnAQuietMoveAndResetsOnAPawnMove() {
        var game = Game.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 5 3")
        assertEquals(5, game.halfmoveClock)

        game = (game.doMove(sq("e1"), sq("d1")) as MoveResult.Success).game
        assertEquals(6, game.halfmoveClock)

        game = (game.doMove(sq("e2"), sq("e4")) as MoveResult.Success).game
        assertEquals(0, game.halfmoveClock)
    }

    @Test
    fun fullmoveNumberIncrementsAfterBlackMoves() {
        var game = Game.fromFen("4k3/4p3/8/8/8/8/4P3/4K3 w - - 0 5")
        assertEquals(5, game.fullmoveNumber)

        game = (game.doMove(sq("e1"), sq("d1")) as MoveResult.Success).game
        assertEquals(5, game.fullmoveNumber)

        game = (game.doMove(sq("e7"), sq("e6")) as MoveResult.Success).game
        assertEquals(6, game.fullmoveNumber)
    }

    @Test
    fun fenRoundTripsAfterAFewMovesFromTheStandardStart() {
        var game = Game()
        game = (game.doMove(sq("e2"), sq("e4")) as MoveResult.Success).game
        game = (game.doMove(sq("e7"), sq("e5")) as MoveResult.Success).game
        game = (game.doMove(sq("g1"), sq("f3")) as MoveResult.Success).game

        val reloaded = Game.fromFen(game.fen)
        // Compare placement, not raw Board equality: Board.fromFen assigns fresh ids from each
        // piece's current file, so a piece that has changed files round-trips to a new id even
        // though its type, color and square are unchanged - fen captures exactly what FEN does.
        assertEquals(game.board.fen, reloaded.board.fen)
        assertEquals(game.turn, reloaded.turn)
        assertEquals(game.fen, reloaded.fen)
    }
}
