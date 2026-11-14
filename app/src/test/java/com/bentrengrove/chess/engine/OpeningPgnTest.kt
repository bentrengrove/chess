package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningPgnTest {
    private fun Opening.lineSans(): List<List<String?>> = lines().map { line -> line.map { it.san } }

    private fun OpeningNode.child(vararg sans: String): OpeningNode =
        sans.fold(this) { node, san ->
            node.children.firstOrNull { it.san == san } ?: throw AssertionError("No child '$san' under '${node.san}'")
        }

    @Test
    fun parsesASingleLine() {
        val opening = Opening.fromPgn("1. e4 e5 2. Nf3 Nc6 3. Bb5", name = "Ruy Lopez", side = PieceColor.White)

        assertEquals("Ruy Lopez", opening.name)
        assertEquals(PieceColor.White, opening.side)
        assertNull(opening.root.san)
        assertEquals(listOf(listOf("e4", "e5", "Nf3", "Nc6", "Bb5")), opening.lineSans())
    }

    @Test
    fun takesNameAndSideFromHeadersWhenNotPassed() {
        val pgn =
            """
            [Event "Sicilian Defence"]
            [Side "Black"]

            1. e4 c5 *
            """.trimIndent()

        val opening = Opening.fromPgn(pgn)

        assertEquals("Sicilian Defence", opening.name)
        assertEquals(PieceColor.Black, opening.side)
        assertEquals(listOf(listOf("e4", "c5")), opening.lineSans())
    }

    @Test
    fun argumentsOverrideHeaders() {
        val opening = Opening.fromPgn("[Event \"Header\"]\n[Side \"Black\"]\n1. e4", name = "Arg", side = PieceColor.White)

        assertEquals("Arg", opening.name)
        assertEquals(PieceColor.White, opening.side)
    }

    @Test
    fun missingNameOrSideIsRejected() {
        val noName = assertThrows(IllegalArgumentException::class.java) { Opening.fromPgn("1. e4", side = PieceColor.White) }
        assertTrue(noName.message!!.contains("[Event]"))

        val noSide = assertThrows(IllegalArgumentException::class.java) { Opening.fromPgn("1. e4", name = "x") }
        assertTrue(noSide.message!!.contains("[Side"))
    }

    @Test
    fun parsesOneBranchPointWithMainLineFirst() {
        val opening = Opening.fromPgn("1. e4 e5 (1... c5 2. Nf3) 2. Nf3", name = "x", side = PieceColor.White)

        val afterE4 = opening.root.child("e4")
        assertEquals(listOf("e5", "c5"), afterE4.children.map { it.san })
        assertEquals(listOf("Nf3"), afterE4.child("c5").children.map { it.san })
        assertEquals(listOf(listOf("e4", "e5", "Nf3"), listOf("e4", "c5", "Nf3")), opening.lineSans())
    }

    @Test
    fun parsesNestedVariations() {
        val pgn = "1. e4 e5 (1... c5 2. Nf3 d6 (2... Nc6 3. d4) (2... e6 3. d4) 3. d4) 2. Nf3 Nc6 (2... d6 3. d4) 3. Bb5"

        val opening = Opening.fromPgn(pgn, name = "x", side = PieceColor.White)

        assertEquals(
            listOf("d6", "Nc6", "e6"),
            opening.root
                .child("e4", "c5", "Nf3")
                .children
                .map { it.san },
        )
        assertEquals(
            listOf(
                listOf("e4", "e5", "Nf3", "Nc6", "Bb5"),
                listOf("e4", "e5", "Nf3", "d6", "d4"),
                listOf("e4", "c5", "Nf3", "d6", "d4"),
                listOf("e4", "c5", "Nf3", "Nc6", "d4"),
                listOf("e4", "c5", "Nf3", "e6", "d4"),
            ),
            opening.lineSans(),
        )
    }

    @Test
    fun attachesCommentsToTheMoveTheyFollow() {
        val pgn =
            """
            {Intro to the opening.} 1. e4 {King's pawn.} e5 2. Nf3
            ({Alternatively} 2. Bc4 {Bishop's opening.}) 2... Nc6 {Defends e5.}
            """.trimIndent()

        val opening = Opening.fromPgn(pgn, name = "x", side = PieceColor.Black)

        assertEquals("Intro to the opening.", opening.root.comment)
        assertEquals("King's pawn.", opening.root.child("e4").comment)
        assertNull(opening.root.child("e4", "e5").comment)
        assertNull(opening.root.child("e4", "e5", "Nf3").comment)
        assertEquals("Defends e5.", opening.root.child("e4", "e5", "Nf3", "Nc6").comment)
        assertEquals("Alternatively Bishop's opening.", opening.root.child("e4", "e5", "Bc4").comment)
    }

    @Test
    fun handlesMoveNumbersAndBlackContinuationsAfterAVariationCloses() {
        val pgn = "1.e4 e5 2.Nf3 Nc6 3.Bc4 (3.Bb5 a6) 3...Bc5 4.c3 (4.O-O Nf6) 4...Nf6"

        val opening = Opening.fromPgn(pgn, name = "x", side = PieceColor.Black)

        assertEquals(
            listOf(
                listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5", "c3", "Nf6"),
                listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5", "O-O", "Nf6"),
                listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6"),
            ),
            opening.lineSans(),
        )
    }

    @Test
    fun ignoresNagsAnnotationsAndResultTokens() {
        val pgn = "1. e4! \$1 e5?! 2. Nf3 !! Nc6 \$6 3. Bb5+? ; line comment 3. d4\n1-0"

        val opening = Opening.fromPgn(pgn, name = "x", side = PieceColor.White)

        assertEquals(listOf(listOf("e4", "e5", "Nf3", "Nc6", "Bb5+")), opening.lineSans())
    }

    @Test
    fun ignoresEachResultToken() {
        listOf("*", "1-0", "0-1", "1/2-1/2").forEach { result ->
            val opening = Opening.fromPgn("1. d4 d5 $result", name = "x", side = PieceColor.White)
            assertEquals(listOf(listOf("d4", "d5")), opening.lineSans())
        }
    }

    @Test
    fun rejectsAnIllegalMoveNamingTheMoveAndItsPath() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                Opening.fromPgn("1. e4 e5 (1... c5 2. Nf3 Ke6) 2. Nf3", name = "x", side = PieceColor.White)
            }

        assertTrue(error.message, error.message!!.contains("'Ke6'"))
        assertTrue(error.message, error.message!!.contains("1.e4 c5 2.Nf3"))
    }

    @Test
    fun rejectsAnUnparseableMove() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                Opening.fromPgn("1. e4 zz9", name = "x", side = PieceColor.White)
            }

        assertTrue(error.message, error.message!!.contains("'zz9'"))
        assertTrue(error.message, error.message!!.contains("1.e4"))
    }

    @Test
    fun rejectsABranchOnTheLearnersSide() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                Opening.fromPgn("1. e4 e5 2. Nf3 (2. Bc4) 2... Nc6", name = "x", side = PieceColor.White)
            }

        assertTrue(error.message, error.message!!.contains("learner"))
        assertTrue(error.message, error.message!!.contains("'Bc4'"))
        assertTrue(error.message, error.message!!.contains("1.e4 e5"))
    }

    @Test
    fun rejectsABranchOnTheLearnersFirstMoveAsBlack() {
        assertThrows(IllegalArgumentException::class.java) {
            Opening.fromPgn("1. e4 e5 (1... c5)", name = "x", side = PieceColor.Black)
        }
    }

    @Test
    fun allowsTheOpponentToBranch() {
        val opening = Opening.fromPgn("1. e4 (1. d4 d5) (1. c4 e5) 1... e5", name = "x", side = PieceColor.Black)

        assertEquals(listOf("e4", "d4", "c4"), opening.root.children.map { it.san })
    }

    @Test
    fun mergesARepeatedMoveIntoTheExistingNode() {
        val opening = Opening.fromPgn("1. e4 e5 2. Nf3 (2. Nf3 Nc6)", name = "x", side = PieceColor.White)

        assertEquals(
            1,
            opening.root
                .child("e4", "e5")
                .children.size,
        )
        assertEquals(listOf(listOf("e4", "e5", "Nf3", "Nc6")), opening.lineSans())
    }

    @Test
    fun rejectsUnbalancedVariations() {
        assertThrows(IllegalArgumentException::class.java) {
            Opening.fromPgn("1. e4 e5 (1... c5", name = "x", side = PieceColor.White)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Opening.fromPgn("1. e4 e5) 2. Nf3", name = "x", side = PieceColor.White)
        }
    }

    @Test
    fun rejectsAFenStartPosition() {
        assertThrows(IllegalArgumentException::class.java) {
            Opening.fromPgn("[FEN \"8/8/8/8/8/8/8/K6k w - - 0 1\"]\n1. Kb1", name = "x", side = PieceColor.White)
        }
    }

    @Test(timeout = 5_000)
    fun rejectsAStrayClosingBraceInsteadOfHanging() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                Opening.fromPgn("1. e4 } e5", name = "x", side = PieceColor.White)
            }
        assertTrue(error.message!!.contains("Unmatched '}'"))
    }

    @Test
    fun linesAreDeterministicAndCoverEveryLeaf() {
        val pgn = "1. e4 e5 (1... c5 2. Nf3) (1... e6 2. d4) 2. Nf3"
        val first = Opening.fromPgn(pgn, name = "x", side = PieceColor.White)
        val second = Opening.fromPgn(pgn, name = "x", side = PieceColor.White)

        assertEquals(first, second)
        assertEquals(first.lines(), second.lines())
        assertEquals(
            listOf(listOf("e4", "e5", "Nf3"), listOf("e4", "c5", "Nf3"), listOf("e4", "e6", "d4")),
            first.lineSans(),
        )
        first.lines().forEach { line -> assertTrue(line.last().children.isEmpty()) }
    }

    @Test
    fun anOpeningWithNoMovesHasNoLines() {
        val opening = Opening.fromPgn("*", name = "x", side = PieceColor.White)

        assertEquals(emptyList<List<OpeningNode>>(), opening.lines())
    }
}
