package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * AI.kt is explicitly commented "doesn't work at all currently" - its search doesn't filter
 * out moves that leave the mover's own king in check (see GameCheckSafetyTest: doMove still
 * returns MoveResult.Success even when it silently rejects a move), so it isn't a reliable move
 * chooser yet. These tests stay at the "doesn't crash, returns a structurally sane move" level
 * rather than asserting anything about move quality, so they won't fight a future rewrite of
 * the search itself.
 */
class AITest {
    @Test
    fun calculateNextMoveReturnsAMoveOnTheStartingPosition() {
        val ai = AI(PieceColor.White)
        val move = ai.calculateNextMove(Game(), PieceColor.White)
        assertNotNull(move)
    }

    @Test
    fun calculateNextMovePicksAMoveOriginatingFromOneOfItsOwnPieces() {
        val ai = AI(PieceColor.White)
        val move = ai.calculateNextMove(Game(), PieceColor.White)
        assertEquals(PieceColor.White, Game().board.pieceAt(move!!.from)?.color)
    }

    @Test
    fun calculateNextMoveWorksForBlackToo() {
        val game = (Game().doMove(sq("e2"), sq("e4")) as MoveResult.Success).game
        val ai = AI(PieceColor.Black)
        val move = ai.calculateNextMove(game, PieceColor.Black)
        assertNotNull(move)
        assertEquals(PieceColor.Black, game.board.pieceAt(move!!.from)?.color)
    }
}
