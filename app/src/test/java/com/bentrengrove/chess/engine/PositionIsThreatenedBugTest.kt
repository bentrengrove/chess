package com.bentrengrove.chess.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * `Game.positionIsThreatened` has a `return` (not `return@find`) inside the lambda passed to
 * `Iterable.find` (Game.kt, around the `piece.type == Pawn` branch). Because `find` is inline,
 * that bare `return` exits `positionIsThreatened` itself the moment it reaches the *first*
 * non-pawn piece of the queried color in board-scan order (rank 8 down to rank 1, a-file to
 * h-file within a rank) - it never looks at any other piece, whether or not that first piece
 * actually threatens the square in question.
 *
 * This test pins the bug down with a minimal repro: a black king on e8 (scanned first) and a
 * black rook on f8 (scanned second) both attack f1's file/rank in real chess, but the rook
 * genuinely threatens f1 while the king does not. The function should say "threatened" but
 * says "safe" instead, because it stops at the king and never reaches the rook.
 *
 * `Game.kingIsInCheck` is unaffected - it goes through `pieceIsThreatenedAt`, which scans all
 * 64 squares with a real (non-short-circuiting) loop instead of `find{}`. The only current
 * caller of the buggy path is `castlingPermitted`; GameCastlingTest's "through check" tests
 * deliberately order pieces so the real attacker is scanned first and sidestep this bug -
 * see the comments there.
 */
class PositionIsThreatenedBugTest {
    @Test
    fun documentsBug_stopsAtFirstNonPawnPieceInsteadOfCheckingAll() {
        val board = customBoard("e8" to "BK4", "f8" to "BR5")
        val game = customGame(board, PieceColor.White)

        // Correct chess: the rook on f8 threatens f1 down a clear file.
        // Buggy engine output: false, because the king (scanned first) doesn't threaten f1,
        // and the scan returns right there without ever checking the rook.
        assertFalse(game.positionIsThreatened(sq("f1"), PieceColor.Black))
    }

    @Test
    @Ignore(
        "engine bug: positionIsThreatened stops at the first non-pawn piece of the queried color in board-scan " +
            "order instead of checking all of them - see documentsBug_stopsAtFirstNonPawnPieceInsteadOfCheckingAll",
    )
    fun spec_squareIsThreatenedWhenAnyPieceOfThatColorCanReachIt() {
        val board = customBoard("e8" to "BK4", "f8" to "BR5")
        val game = customGame(board, PieceColor.White)

        assertTrue(game.positionIsThreatened(sq("f1"), PieceColor.Black))
    }
}
