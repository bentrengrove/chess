package com.bentrengrove.chess.engine

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `Game.positionIsThreatened` scans every piece of the queried color and asks whether any one
 * of them can reach the target square. It used to have a bare `return` (instead of
 * `return@find`) inside the lambda passed to `Iterable.find`, which - because `find` is inline -
 * exited the whole function at the first non-pawn piece of the queried color in board-scan
 * order, ignoring every piece after it. Fixed to `return@find`.
 */
class PositionIsThreatenedTest {
    @Test
    fun squareIsThreatenedByAPieceScannedAfterAnotherNonThreateningPieceOfTheSameColor() {
        // A black king on e8 (scanned first, doesn't reach f1) and a black rook on f8
        // (scanned second, does reach f1 down the open file) - the regression case for the
        // scan-order bug described above.
        val board = customBoard("e8" to "BK4", "f8" to "BR5")
        val game = customGame(board, PieceColor.White)

        assertTrue(game.positionIsThreatened(sq("f1"), PieceColor.Black))
    }
}
