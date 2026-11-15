package com.bentrengrove.chess.openingscreen

import com.bentrengrove.chess.engine.Opening
import com.bentrengrove.chess.engine.PieceColor
import com.bentrengrove.chess.engine.lines
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class LineNamesTest {
    /** Writes expected text with plain spaces; formatMoves uses no-break spaces after numbers. */
    private fun String.nbsp() = replace(Regex("(\\.+) "), "$1\u00A0")

    private fun bundled(fileName: String) = Opening.fromPgn(File("src/main/assets/openings/$fileName").readText()).lines()

    @Test
    fun linesAreNamedByTheMoveWhereTheyBranch() {
        val french = bundled("french.pgn")
        assertEquals(listOf("3. e5", "3. exd5", "3. Nd2", "3. Nc3").map { it.nbsp() }, french.indices.map { lineName(french, it) })

        // Nested variations are named by their deepest branch: 3...exf4 4.e5 Nh5 is "4... Nh5".
        val vienna = bundled("vienna-gambit.pgn")
        val nh5 = vienna.indexOfFirst { line -> line.any { it.san == "Nh5" } }
        assertEquals("4... Nh5".nbsp(), lineName(vienna, nh5))
        assertEquals(
            vienna.size,
            vienna.indices
                .map { lineName(vienna, it) }
                .toSet()
                .size,
        )
    }

    @Test
    fun formatMovesNumbersFromTheStartingPly() {
        val line = bundled("french.pgn")[0]

        assertEquals("1. e4 e6 2. d4".nbsp(), formatMoves(line.take(3), startPly = 0))
        assertEquals("2... d5 3. e5".nbsp(), formatMoves(line.drop(3).take(2), startPly = 3))
    }

    @Test
    fun aSingleLineIsTheMainLine() {
        val lines = Opening.fromPgn("1. e4 e5", name = "One", side = PieceColor.White).lines()

        assertEquals("Main line", lineName(lines, 0))
    }
}
