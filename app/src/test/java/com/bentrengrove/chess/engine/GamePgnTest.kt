package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GamePgnTest {
    @Test
    fun fromPgnReplaysAShortOpeningFromTheStandardStart() {
        val game = Game.fromPgn("1. e4 e5 2. Nf3 Nc6 3. Bb5")

        assertEquals(PieceColor.Black, game.turn)
        assertEquals(PieceType.Pawn, game.board.pieceAt(sq("e4"))?.type)
        assertEquals(PieceType.Pawn, game.board.pieceAt(sq("e5"))?.type)
        assertEquals(PieceType.Knight, game.board.pieceAt(sq("f3"))?.type)
        assertEquals(PieceType.Knight, game.board.pieceAt(sq("c6"))?.type)
        assertEquals(PieceType.Bishop, game.board.pieceAt(sq("b5"))?.type)
        assertNull(game.board.pieceAt(sq("e2")))
        assertNull(game.board.pieceAt(sq("g1")))
    }

    @Test
    fun fromPgnUnderstandsKingSideCastling() {
        val game = Game.fromPgn("1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. O-O")

        assertEquals(PieceType.King, game.board.pieceAt(sq("g1"))?.type)
        assertEquals(PieceType.Rook, game.board.pieceAt(sq("f1"))?.type)
        assertNull(game.board.pieceAt(sq("e1")))
        assertNull(game.board.pieceAt(sq("h1")))
    }

    @Test
    fun fromPgnUnderstandsQueenSideCastling() {
        val game = Game.fromPgn("[FEN \"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1\"]\n\n1. O-O-O")

        assertEquals(PieceType.King, game.board.pieceAt(sq("c1"))?.type)
        assertEquals(PieceType.Rook, game.board.pieceAt(sq("d1"))?.type)
        assertNull(game.board.pieceAt(sq("e1")))
        assertNull(game.board.pieceAt(sq("a1")))
    }

    @Test
    fun fromPgnAppliesCheckAndMateGlyphsAsPlainMoves() {
        // Scholar's mate - also exercises capture ("Qxf7") and mate ("#") suffix handling
        // together with the engine's own (already well-tested) checkmate detection.
        val game = Game.fromPgn("1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7#")

        assertEquals(GameState.CHECKMATE, game.gameState)
        assertEquals(PieceType.Queen, game.board.pieceAt(sq("f7"))?.type)
        assertEquals(PieceColor.White, game.board.pieceAt(sq("f7"))?.color)
    }

    @Test
    fun fromPgnAppliesPromotionUsingTheEqualsSignNotation() {
        val game = Game.fromPgn("[FEN \"4k3/7P/8/8/8/8/8/4K3 w - - 0 1\"]\n\n1. h8=Q")

        assertEquals(PieceType.Queen, game.board.pieceAt(sq("h8"))?.type)
        assertEquals(PieceColor.White, game.board.pieceAt(sq("h8"))?.color)
    }

    @Test
    fun fromPgnAppliesACapturingPromotion() {
        val game = Game.fromPgn("[FEN \"4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1\"]\n\n1. gxh8=Q")

        assertEquals(PieceType.Queen, game.board.pieceAt(sq("h8"))?.type)
        assertNull(game.board.pieceAt(sq("g7")))
    }

    @Test
    fun fromPgnResolvesFileDisambiguationBetweenTwoKnightsThatCanReachTheSameSquare() {
        val game = Game.fromPgn("[FEN \"4k3/8/8/8/8/1N3N2/8/4K3 w - - 0 1\"]\n\n1. Nbd4")

        assertEquals(PieceType.Knight, game.board.pieceAt(sq("d4"))?.type)
        assertNull(game.board.pieceAt(sq("b3")))
        assertEquals(PieceType.Knight, game.board.pieceAt(sq("f3"))?.type)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromPgnRejectsAnAmbiguousMoveWithNoDisambiguation() {
        Game.fromPgn("[FEN \"4k3/8/8/8/8/1N3N2/8/4K3 w - - 0 1\"]\n\n1. Nd4")
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromPgnRejectsCastlingThatIsNotActuallyLegal() {
        // Unlike every other SAN shape, O-O/O-O-O used to skip the canMove/isLegalMove guard
        // and hand bogus coordinates straight to doMove (which does no validation of its own).
        // Castling rights are still "KQ" here - it's the king having moved and returned to e1
        // that makes castling illegal, so only the pieceHasMoved history check (not the rights
        // gate) can catch this.
        Game.fromPgn("[FEN \"4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1\"]\n\n1. Ke2 Kd7 2. Ke1 Kd8 3. O-O")
    }

    @Test
    fun fromPgnIgnoresCommentsAndSidelineVariations() {
        val annotated = Game.fromPgn("1. e4 {best by test} e5 (1...c5 2. Nf3 d6) 2. Nf3 Nc6")
        val plain = Game.fromPgn("1. e4 e5 2. Nf3 Nc6")

        assertEquals(plain.board, annotated.board)
        assertEquals(plain.turn, annotated.turn)
    }

    @Test
    fun fromPgnIgnoresTagPairsAndATrailingResultToken() {
        val game =
            Game.fromPgn(
                """
                [Event "Test"]
                [Site "?"]
                [Result "1-0"]

                1. e4 e5 2. Nf3 Nc6 1-0
                """.trimIndent(),
            )

        assertEquals(PieceType.Pawn, game.board.pieceAt(sq("e4"))?.type)
        assertEquals(PieceType.Knight, game.board.pieceAt(sq("f3"))?.type)
        assertEquals(PieceType.Knight, game.board.pieceAt(sq("c6"))?.type)
    }

    @Test
    fun fromPgnStartsFromAFenTagWhenPresent() {
        val game = Game.fromPgn("[FEN \"4k3/8/8/8/8/8/4P3/4K3 w - - 0 1\"]\n\n1. e4")

        assertEquals(PieceType.Pawn, game.board.pieceAt(sq("e4"))?.type)
        assertNull(game.board.pieceAt(sq("e2")))
        assertNull(game.board.pieceAt(sq("d2")))
    }

    @Test
    fun fromPgnMatchesDoMoveDrivenPlayForTheSameGame() {
        var driven = Game()
        driven = (driven.doMove(sq("e2"), sq("e4")) as MoveResult.Success).game
        driven = (driven.doMove(sq("e7"), sq("e5")) as MoveResult.Success).game
        driven = (driven.doMove(sq("g1"), sq("f3")) as MoveResult.Success).game
        driven = (driven.doMove(sq("b8"), sq("c6")) as MoveResult.Success).game

        val fromPgn = Game.fromPgn("1. e4 e5 2. Nf3 Nc6")

        assertEquals(driven.board, fromPgn.board)
        assertEquals(driven.turn, fromPgn.turn)
    }

    @Test
    fun fromPgnHandlesAPawnCaptureWithFileDisambiguation() {
        val game = Game.fromPgn("1. e4 d5 2. exd5")

        assertEquals(PieceType.Pawn, game.board.pieceAt(sq("d5"))?.type)
        assertEquals(PieceColor.White, game.board.pieceAt(sq("d5"))?.color)
        assertNull(game.board.pieceAt(sq("e4")))
        assertTrue(game.board.allPieces.count { it.second.type == PieceType.Pawn } == 15)
    }
}
