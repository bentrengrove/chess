package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class PositionTest {
    @Test
    fun toAlgebraicConvertsCornersCorrectly() {
        assertEquals("a8", Position(0, 0).toAlgebraic())
        assertEquals("h8", Position(7, 0).toAlgebraic())
        assertEquals("a1", Position(0, 7).toAlgebraic())
        assertEquals("h1", Position(7, 7).toAlgebraic())
        assertEquals("e2", Position(4, 6).toAlgebraic())
    }

    @Test
    fun sqHelperRoundTripsWithToAlgebraic() {
        val squares = listOf("a8", "h8", "a1", "h1", "e4", "d5", "c7")
        squares.forEach { algebraic ->
            assertEquals(algebraic, sq(algebraic).toAlgebraic())
        }
    }

    @Test
    fun positionMinusPositionProducesDelta() {
        assertEquals(Delta(3, -2), Position(5, 1) - Position(2, 3))
    }

    @Test
    fun positionPlusPositionProducesDelta() {
        assertEquals(Delta(7, 4), Position(5, 1) + Position(2, 3))
    }

    @Test
    fun positionPlusDeltaProducesPosition() {
        assertEquals(Position(7, -2), Position(5, 1) + Delta(2, -3))
    }
}
