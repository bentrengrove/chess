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

    /** Asserts there is one line per expected prefix, in PGN order, and each line starts with its prefix. */
    private fun assertLinesStartWith(
        lines: List<List<String>>,
        prefixes: List<List<String>>,
    ) {
        assertEquals("Number of lines in $lines", prefixes.size, lines.size)
        lines.zip(prefixes).forEach { (line, prefix) -> assertEquals(prefix, line.take(prefix.size)) }
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
        assertLinesStartWith(
            lines,
            listOf(
                listOf("e4", "e5", "Nc3", "Nf6", "f4", "d5", "fxe5", "Nxe4", "Nf3", "Be7"),
                listOf("e4", "e5", "Nc3", "Nf6", "f4", "d5", "fxe5", "Nxe4", "Nf3", "Bg4"),
                listOf("e4", "e5", "Nc3", "Nf6", "f4", "exf4", "e5", "Ng8", "Nf3", "d6"),
                listOf("e4", "e5", "Nc3", "Nf6", "f4", "exf4", "e5", "Ng8", "Nf3", "Nc6"),
                listOf("e4", "e5", "Nc3", "Nf6", "f4", "exf4", "e5", "Nh5"),
                listOf("e4", "e5", "Nc3", "Nf6", "f4", "exf4", "e5", "Qe7"),
                listOf("e4", "e5", "Nc3", "Nf6", "f4", "d6", "Nf3", "Nc6"),
            ),
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
        assertLinesStartWith(
            lines,
            listOf(
                listOf("e4", "e6", "d4", "d5", "e5", "c5"),
                listOf("e4", "e6", "d4", "d5", "exd5", "exd5"),
                listOf("e4", "e6", "d4", "d5", "Nd2", "c5"),
                listOf("e4", "e6", "d4", "d5", "Nc3", "Bb4"),
            ),
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
}
