package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun startingMaterialIsThirtyEight() {
        assertEquals(38, Game().valueFor(PieceColor.White))
    }
}
