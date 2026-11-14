package com.bentrengrove.chess.gamescreen

import com.bentrengrove.chess.engine.PieceColor
import com.bentrengrove.chess.engine.sq
import org.junit.Assert.assertEquals
import org.junit.Test

class BoardOrientationTest {
    @Test
    fun whiteOrientation_matchesEngineCoordinates() {
        assertEquals(sq("a8"), positionForCell(0, 0, PieceColor.White))
        assertEquals(sq("h8"), positionForCell(7, 0, PieceColor.White))
        assertEquals(sq("a1"), positionForCell(0, 7, PieceColor.White))
        assertEquals(sq("h1"), positionForCell(7, 7, PieceColor.White))
        assertEquals(sq("e4"), positionForCell(4, 4, PieceColor.White))
    }

    @Test
    fun blackOrientation_rotatesBoard180Degrees() {
        assertEquals(sq("h1"), positionForCell(0, 0, PieceColor.Black))
        assertEquals(sq("a1"), positionForCell(7, 0, PieceColor.Black))
        assertEquals(sq("h8"), positionForCell(0, 7, PieceColor.Black))
        assertEquals(sq("a8"), positionForCell(7, 7, PieceColor.Black))
        assertEquals(sq("e4"), positionForCell(3, 3, PieceColor.Black))
    }

    @Test
    fun cellForPosition_isInverseOfPositionForCell() {
        for (orientation in listOf(PieceColor.White, PieceColor.Black)) {
            for (row in 0 until 8) {
                for (column in 0 until 8) {
                    val position = positionForCell(column, row, orientation)
                    assertEquals(column to row, cellForPosition(position, orientation))
                }
            }
        }
    }
}
