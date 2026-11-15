package com.bentrengrove.chess.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class OpeningTrainerTest {
    /** White repertoire with two lines: e4 e5 Nf3 Nc6 Bb5, and e4 c5 Nf3. */
    private val white =
        Opening.fromPgn(
            "{Intro.} 1. e4 {King's pawn.} e5 (1... c5 2. Nf3) 2. Nf3 Nc6 3. Bb5",
            name = "Test White",
            side = PieceColor.White,
        )

    /** Black repertoire with two lines: e4 e6 d4 d5, and e4 e6 d3 d5. */
    private val black =
        Opening.fromPgn(
            "1. e4 e6 2. d4 (2. d3 d5) d5",
            name = "Test Black",
            side = PieceColor.Black,
        )

    private val french = Opening.fromPgn(File("src/main/assets/openings/french.pgn").readText())

    private fun OpeningTrainer.playExpected(): OpeningTrainer =
        if (isOpponentTurn) {
            playOpponentMove()
        } else {
            val move = expectedMove!!
            playMove(move.from, move.to)
        }

    private fun OpeningTrainer.completeLine(): OpeningTrainer {
        var trainer = this
        while (!trainer.isLineComplete) trainer = trainer.playExpected()
        return trainer
    }

    // Checking moves

    @Test
    fun learnStartsOnTheFirstLineWithTheLearnerToMove() {
        val trainer = OpeningTrainer.start(white)

        assertEquals(TrainerMode.Learn, trainer.mode)
        assertEquals(0, trainer.lineIndex)
        assertEquals(2, trainer.lines.size)
        assertTrue(trainer.isLearnerTurn)
        assertEquals(Move(sq("e2"), sq("e4")), trainer.expectedMove)
    }

    @Test
    fun theExpectedMoveAdvancesTheLine() {
        val trainer = OpeningTrainer.start(white).playMove(sq("e2"), sq("e4"))

        assertEquals(1, trainer.ply)
        assertEquals(Game.fromPgn("1. e4"), trainer.game)
        assertTrue(trainer.isOpponentTurn)
    }

    @Test
    fun aLegalButWrongMoveIsRejectedAndCounted() {
        val start = OpeningTrainer.start(white)
        val trainer = start.playMove(sq("d2"), sq("d4"))

        assertEquals(0, trainer.ply)
        assertEquals(start.game, trainer.game)
        assertEquals(1, trainer.wrongMoveCount)
        assertTrue(trainer.isLearnerTurn)
    }

    @Test
    fun learnModeRejectsWithoutMarkingTheMoveMissed() {
        val trainer = OpeningTrainer.start(white).playMove(sq("d2"), sq("d4"))

        assertFalse(trainer.currentMoveMissed)
    }

    @Test
    fun anExpectedCastlingMoveIsAccepted() {
        val opening =
            Opening.fromPgn("1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. O-O", name = "Castle", side = PieceColor.White)
        var trainer = OpeningTrainer.start(opening)
        repeat(6) { trainer = trainer.playExpected() }

        trainer = trainer.playMove(sq("e1"), sq("g1"))

        assertTrue(trainer.isLineComplete)
        assertEquals(Game.fromPgn("1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. O-O"), trainer.game)
    }

    // Opponent replies

    @Test
    fun theOpponentPlaysTheNextMoveOfTheLine() {
        val trainer = OpeningTrainer.start(white).playExpected().playOpponentMove()

        assertEquals(Game.fromPgn("1. e4 e5"), trainer.game)
        assertTrue(trainer.isLearnerTurn)
    }

    @Test
    fun theOpponentFollowsTheSelectedLine() {
        val trainer =
            OpeningTrainer
                .start(white)
                .selectLine(1)
                .playExpected()
                .playOpponentMove()

        assertEquals(Game.fromPgn("1. e4 c5"), trainer.game)
    }

    @Test
    fun movesAreIgnoredOnTheWrongTurn() {
        val opponentTurn = OpeningTrainer.start(white).playExpected()
        assertSame(opponentTurn, opponentTurn.playMove(sq("e7"), sq("e5")))

        val learnerTurn = OpeningTrainer.start(white)
        assertSame(learnerTurn, learnerTurn.playOpponentMove())
    }

    @Test
    fun playingBlackTheOpponentMovesFirst() {
        val trainer = OpeningTrainer.start(french)

        assertTrue(trainer.isOpponentTurn)
        assertNull(trainer.highlightedFrom)

        val afterWhite = trainer.playOpponentMove()
        assertEquals(Game.fromPgn("1. e4"), afterWhite.game)
        assertTrue(afterWhite.isLearnerTurn)
        assertEquals(Move(sq("e7"), sq("e6")), afterWhite.expectedMove)
    }

    // Highlights and hints

    @Test
    fun learnModeHighlightsTheExpectedMove() {
        val trainer = OpeningTrainer.start(white)

        assertEquals(sq("e2"), trainer.highlightedFrom)
        assertEquals(sq("e4"), trainer.highlightedTo)
    }

    @Test
    fun drillModeHighlightsNothingUntilAHint() {
        val trainer = OpeningTrainer.start(white, TrainerMode.Drill, Random(0))

        assertNull(trainer.highlightedFrom)
        assertNull(trainer.highlightedTo)
    }

    @Test
    fun theFirstHintShowsThePieceAndTheSecondTheFullMove() {
        val start = OpeningTrainer.start(white, TrainerMode.Drill, Random(0))

        val oneHint = start.requestHint()
        assertEquals(HintLevel.Piece, oneHint.hint)
        assertEquals(sq("e2"), oneHint.highlightedFrom)
        assertNull(oneHint.highlightedTo)

        val twoHints = oneHint.requestHint()
        assertEquals(HintLevel.Move, twoHints.hint)
        assertEquals(sq("e2"), twoHints.highlightedFrom)
        assertEquals(sq("e4"), twoHints.highlightedTo)
    }

    @Test
    fun aHintMarksTheMoveMissed() {
        val trainer = OpeningTrainer.start(white, TrainerMode.Drill, Random(0)).requestHint().playExpected()

        assertEquals(mapOf("e4" to false), trainer.moveResults)
        assertEquals(1, trainer.lineMistakes)
        assertEquals(HintLevel.None, trainer.hint)
        assertFalse(trainer.currentMoveMissed)
    }

    @Test
    fun hintsAreIgnoredInLearnMode() {
        val trainer = OpeningTrainer.start(white)

        assertSame(trainer, trainer.requestHint())
    }

    @Test
    fun aWrongMoveInDrillIsRetriedWithoutRevealingTheAnswer() {
        val wrong = OpeningTrainer.start(white, TrainerMode.Drill, Random(0)).playMove(sq("d2"), sq("d4"))

        assertTrue(wrong.currentMoveMissed)
        assertTrue(wrong.isLearnerTurn)
        assertNull(wrong.highlightedFrom)
        assertNull(wrong.highlightedTo)

        val right = wrong.playExpected()
        assertEquals(mapOf("e4" to false), right.moveResults)
    }

    @Test
    fun aMoveRightFirstTimeIsRecorded() {
        val trainer = OpeningTrainer.start(white, TrainerMode.Drill, Random(0)).playExpected()

        assertEquals(mapOf("e4" to true), trainer.moveResults)
        assertEquals(0, trainer.lineMistakes)
    }

    // End of a line

    @Test
    fun theEndOfALineIsDetected() {
        val trainer = OpeningTrainer.start(white).completeLine()

        assertTrue(trainer.isLineComplete)
        assertFalse(trainer.isLearnerTurn)
        assertFalse(trainer.isOpponentTurn)
        assertNull(trainer.expectedMove)
        assertEquals(Game.fromPgn("1. e4 e5 2. Nf3 Nc6 3. Bb5"), trainer.game)
    }

    @Test
    fun finishingADrillLineCoversItAndRecordsTheResult() {
        val start = OpeningTrainer.start(white, TrainerMode.Drill, Random(0)).selectLine(0)
        val clean = start.completeLine()

        assertEquals(setOf(0), clean.coveredLines)
        assertEquals(mapOf("e4 e5 Nf3 Nc6 Bb5" to true), clean.lineResults)
        assertEquals(
            mapOf("e4" to true, "e4 e5 Nf3" to true, "e4 e5 Nf3 Nc6 Bb5" to true),
            clean.moveResults,
        )

        val withMistake = start.playMove(sq("d2"), sq("d4")).completeLine()
        assertEquals(1, withMistake.lineMistakes)
        assertEquals(mapOf("e4 e5 Nf3 Nc6 Bb5" to false), withMistake.lineResults)
    }

    @Test
    fun finishingALearnLineDoesNotCoverIt() {
        val trainer = OpeningTrainer.start(white).completeLine()

        assertTrue(trainer.coveredLines.isEmpty())
        assertTrue(trainer.lineResults.isEmpty())
        assertTrue(trainer.moveResults.isEmpty())
    }

    // Picking lines

    @Test
    fun drillCoversEveryLineBeforeRepeatingAny() {
        repeat(20) { seed ->
            val random = Random(seed)
            var trainer = OpeningTrainer.start(french, TrainerMode.Drill, random)
            val picked = mutableListOf<Int>()
            repeat(trainer.lines.size) {
                picked += trainer.lineIndex
                trainer = trainer.completeLine().nextLine(random)
            }

            assertEquals("seed $seed picked $picked", trainer.lines.indices.toSet(), picked.toSet())
            assertEquals(trainer.lines.indices.toSet(), trainer.coveredLines)
        }
    }

    @Test
    fun onceEveryLineIsCoveredDrillPicksAnyLine() {
        val random = Random(1)
        var trainer = OpeningTrainer.start(black, TrainerMode.Drill, random)
        repeat(2) { trainer = trainer.completeLine().nextLine(random) }

        val picks =
            (0 until 50)
                .map {
                    trainer = trainer.completeLine().nextLine(random)
                    trainer.lineIndex
                }.toSet()

        assertEquals(setOf(0, 1), picks)
    }

    @Test
    fun learnNextLineGoesInOrderAndWraps() {
        val trainer = OpeningTrainer.start(white)

        assertEquals(1, trainer.nextLine().lineIndex)
        assertEquals(0, trainer.nextLine().nextLine().lineIndex)
    }

    @Test
    fun selectingALineRestartsIt() {
        val trainer =
            OpeningTrainer
                .start(white)
                .playExpected()
                .playExpected()
                .selectLine(1)

        assertEquals(1, trainer.lineIndex)
        assertEquals(0, trainer.ply)
        assertEquals(Game(), trainer.game)
    }

    @Test
    fun switchingModeKeepsResults() {
        val drilled = OpeningTrainer.start(white, TrainerMode.Drill, Random(0)).selectLine(0).completeLine()
        val learning = drilled.withMode(TrainerMode.Learn)

        assertEquals(TrainerMode.Learn, learning.mode)
        assertEquals(0, learning.lineIndex)
        assertEquals(0, learning.ply)
        assertEquals(drilled.coveredLines, learning.coveredLines)
        assertEquals(drilled.moveResults, learning.moveResults)
    }

    // Undo and comments

    @Test
    fun undoGoesBackToTheLearnersPreviousMove() {
        // e4 e5 Nf3 played, so it's Black's turn after the learner's Nf3.
        val trainer =
            OpeningTrainer
                .start(white)
                .playExpected()
                .playExpected()
                .playExpected()
        assertTrue(trainer.canUndo)

        val undone = trainer.undo()
        assertEquals(2, undone.ply)
        assertEquals(Game.fromPgn("1. e4 e5"), undone.game)
        assertTrue(undone.isLearnerTurn)

        val undoneAgain = undone.undo()
        assertEquals(0, undoneAgain.ply)
        assertFalse(undoneAgain.canUndo)
        assertSame(undoneAgain, undoneAgain.undo())
    }

    @Test
    fun undoAtTheEndOfALineReplaysTheLastMove() {
        val trainer = OpeningTrainer.start(white).completeLine().undo()

        assertEquals(4, trainer.ply)
        assertEquals(Move(sq("f1"), sq("b5")), trainer.expectedMove)
    }

    @Test
    fun playingBlackUndoIsUnavailableBeforeTheFirstLearnerMove() {
        val trainer = OpeningTrainer.start(black).playOpponentMove()

        assertFalse(trainer.canUndo)
    }

    @Test
    fun undoIsUnavailableInDrillMode() {
        val trainer = OpeningTrainer.start(white, TrainerMode.Drill, Random(0)).playExpected().playExpected()

        assertFalse(trainer.canUndo)
        assertSame(trainer, trainer.undo())
    }

    @Test
    fun theCommentIsTheIntroThenTheMoveJustPlayed() {
        val start = OpeningTrainer.start(white)
        assertEquals("Intro.", start.currentComment)

        assertEquals("King's pawn.", start.playExpected().currentComment)
        assertNull(start.playExpected().playExpected().currentComment)
    }
}
