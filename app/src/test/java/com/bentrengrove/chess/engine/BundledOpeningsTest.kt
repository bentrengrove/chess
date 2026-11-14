package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Loads each opening bundled in `app/src/main/assets/openings/` and checks it parses into a valid
 * repertoire: every move legal, the learner never branching, and the expected main branches.
 */
class BundledOpeningsTest {
    // Gradle runs unit tests with the module directory (app/) as the working directory.
    private fun loadOpening(fileName: String): Opening = Opening.fromPgn(File("src/main/assets/openings/$fileName").readText())

    private fun Opening.lineSans(): List<List<String>> = lines().map { line -> line.map { it.san!! } }

    /** Replays every line and asserts a SAN ends in '+' exactly when the move gives check. */
    private fun assertCheckMarkersAreAccurate(opening: Opening) {
        opening.lineSans().forEach { line ->
            line.fold(Game()) { game, san ->
                val next = game.applySanMove(san)
                val givesCheck = next.gameState == GameState.CHECK || next.gameState == GameState.CHECKMATE
                assertEquals("Check marker on '$san' in $line", givesCheck, san.endsWith("+") || san.endsWith("#"))
                next
            }
        }
    }

    /** Every line should end on the learner's move, so a drill finishes with the learner playing. */
    private fun assertLinesEndOnLearnersMove(opening: Opening) {
        val learnerParity = if (opening.side == PieceColor.White) 1 else 0
        opening.lineSans().forEach { line ->
            assertEquals("Line $line should end on the learner's move", learnerParity, line.size % 2)
        }
    }

    @Test
    fun viennaGambitParsesIntoTheExpectedRepertoire() {
        val opening = loadOpening("vienna-gambit.pgn")

        assertEquals("Vienna Gambit", opening.name)
        assertEquals(PieceColor.White, opening.side)
        assertFalse(opening.root.comment.isNullOrBlank())

        val lines = opening.lineSans()
        assertEquals(4, lines.size)
        assertEquals(
            listOf(
                listOf("e4", "e5", "Nc3", "Nf6", "f4", "d5", "fxe5", "Nxe4", "Nf3", "Be7"),
                listOf("e4", "e5", "Nc3", "Nf6", "f4", "d5", "fxe5", "Nxe4", "Nf3", "Bg4"),
                listOf("e4", "e5", "Nc3", "Nf6", "f4", "exf4", "e5", "Ng8"),
                listOf("e4", "e5", "Nc3", "Nf6", "f4", "d6", "Nf3", "Nc6"),
            ),
            lines.map { it.take(it.indexOfMainBranchEnd()) },
        )
        lines.forEach { assertTrue("Line $it is too short", it.size >= 16) }

        assertCheckMarkersAreAccurate(opening)
        assertLinesEndOnLearnersMove(opening)
    }

    @Test
    fun frenchDefenceParsesIntoTheExpectedRepertoire() {
        val opening = loadOpening("french.pgn")

        assertEquals("French Defence", opening.name)
        assertEquals(PieceColor.Black, opening.side)
        assertFalse(opening.root.comment.isNullOrBlank())

        val lines = opening.lineSans()
        assertEquals(4, lines.size)
        assertEquals(
            listOf(
                listOf("e4", "e6", "d4", "d5", "e5", "c5"),
                listOf("e4", "e6", "d4", "d5", "exd5", "exd5"),
                listOf("e4", "e6", "d4", "d5", "Nd2", "c5"),
                listOf("e4", "e6", "d4", "d5", "Nc3", "Bb4"),
            ),
            lines.map { it.take(6) },
        )
        lines.forEach { assertTrue("Line $it is too short", it.size >= 16) }

        assertCheckMarkersAreAccurate(opening)
        assertLinesEndOnLearnersMove(opening)
    }

    @Test
    fun keyMovesCarryComments() {
        listOf("vienna-gambit.pgn", "french.pgn").forEach { fileName ->
            val opening = loadOpening(fileName)
            val commented =
                opening
                    .lines()
                    .flatten()
                    .distinct()
                    .count { !it.comment.isNullOrBlank() }
            assertTrue("$fileName should comment its key moves, found $commented", commented >= 10)
        }
    }

    // The Vienna branches at Black's 3rd move and again at Black's 5th in the 3...d5 line, so
    // the 3...d5 lines are identified by 10 plies and the others by 8.
    private fun List<String>.indexOfMainBranchEnd(): Int = if (this[5] == "d5") 10 else 8
}
