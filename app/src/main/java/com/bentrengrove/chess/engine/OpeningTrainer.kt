package com.bentrengrove.chess.engine

import kotlin.random.Random

/** How an [OpeningTrainer] presents a line: [Learn] shows each move, [Drill] tests from memory. */
enum class TrainerMode {
    Learn,
    Drill,
}

/** How much of the learner's next move a Drill hint reveals. */
enum class HintLevel {
    None,

    /** The piece to move. */
    Piece,

    /** The piece to move and the square it goes to. */
    Move,
}

/**
 * Practising an [Opening] one line at a time, in the immutable style of [Game]: every action
 * returns a new trainer.
 *
 * The learner plays [Opening.side] and must find the one move the repertoire gives at each of
 * their turns; the opponent's moves follow the current line (see [playOpponentMove]).
 *
 * Results are tracked per move and per line in [moveResults] and [lineResults], keyed by SAN path
 * (e.g. "e4 e6 d4 d5") so they can be saved and matched up again later.
 *
 * @property lineIndex the current line, an index into [lines].
 * @property ply how many moves of the current line have been played.
 * @property game the position after those moves.
 * @property hint how much of the learner's next move is revealed (Drill only).
 * @property currentMoveMissed whether the learner has already got the next move wrong or used a hint.
 * @property lineMistakes how many learner moves in the current line weren't right first time.
 * @property wrongMoveCount total rejected moves, which goes up by one on each rejection so the UI
 * can react to it.
 * @property coveredLines indices of the lines completed in Drill mode this session.
 * @property moveResults for each learner move drilled, keyed by the SAN path up to and including
 * it, whether it was right first time on the latest attempt.
 * @property lineResults for each line drilled, keyed by its full SAN path, whether the latest
 * attempt had no mistakes.
 */
data class OpeningTrainer(
    val opening: Opening,
    val mode: TrainerMode = TrainerMode.Learn,
    val lineIndex: Int = 0,
    val ply: Int = 0,
    val game: Game = Game(),
    val hint: HintLevel = HintLevel.None,
    val currentMoveMissed: Boolean = false,
    val lineMistakes: Int = 0,
    val wrongMoveCount: Int = 0,
    val coveredLines: Set<Int> = emptySet(),
    val moveResults: Map<String, Boolean> = emptyMap(),
    val lineResults: Map<String, Boolean> = emptyMap(),
) {
    /** Every line of the opening, in [Opening.lines] order. */
    val lines: List<List<OpeningNode>> by lazy { opening.lines() }

    /** The moves of the current line. */
    val line: List<OpeningNode>
        get() = lines.getOrElse(lineIndex) { emptyList() }

    /** True once every move of the current line has been played. */
    val isLineComplete: Boolean
        get() = ply >= line.size

    /** True when the learner has a move to make. */
    val isLearnerTurn: Boolean
        get() = !isLineComplete && game.turn == opening.side

    /** True when the opponent has a move to make; see [playOpponentMove]. */
    val isOpponentTurn: Boolean
        get() = !isLineComplete && game.turn != opening.side

    /** The next move of the current line, or null at the end of the line. */
    val expectedMove: Move?
        get() = expectedGame?.history?.last()

    private val expectedGame: Game?
        get() = line.getOrNull(ply)?.san?.let { game.applySanMove(it) }

    /**
     * The comments to show for the current position: those on the learner's latest move and the
     * opponent's reply. Before the learner has moved, the opening's introduction comes first, so
     * playing Black still sees it.
     */
    val currentComments: List<String>
        get() {
            val lastLearnerPly = (ply - 1 downTo 0).firstOrNull { isLearnerPly(it) }
            val moveComments = line.subList(lastLearnerPly ?: 0, ply.coerceAtMost(line.size)).mapNotNull { it.comment }
            return if (lastLearnerPly == null) listOfNotNull(opening.root.comment) + moveComments else moveComments
        }

    /**
     * The square of the piece to highlight for the learner: in Learn mode the piece to move,
     * in Drill mode only once a hint is used.
     */
    val highlightedFrom: Position?
        get() =
            when {
                !isLearnerTurn -> null
                mode == TrainerMode.Learn || hint != HintLevel.None -> expectedMove?.from
                else -> null
            }

    /** The destination square to highlight: in Learn mode always, in Drill mode after a second hint. */
    val highlightedTo: Position?
        get() =
            when {
                !isLearnerTurn -> null
                mode == TrainerMode.Learn || hint == HintLevel.Move -> expectedMove?.to
                else -> null
            }

    /** True when [undo] would go back to an earlier learner move. */
    val canUndo: Boolean
        get() = mode == TrainerMode.Learn && previousLearnerPly() != null

    /**
     * Plays the learner's move [from] -> [to]. The expected move advances the line; anything else,
     * or a move made out of turn, is rejected and leaves the position unchanged. A rejected move
     * bumps [wrongMoveCount] and, in Drill mode, marks the current move as missed.
     */
    fun playMove(
        from: Position,
        to: Position,
    ): OpeningTrainer {
        if (!isLearnerTurn) return this
        val played =
            when (val result = game.doMove(from, to)) {
                is MoveResult.Success -> result.game
                is MoveResult.Promotion -> (result.onPieceSelection(PieceType.Queen) as MoveResult.Success).game
            }
        val expected = expectedGame
        if (played != expected) {
            return copy(
                wrongMoveCount = wrongMoveCount + 1,
                currentMoveMissed = currentMoveMissed || mode == TrainerMode.Drill,
            )
        }

        val withResult =
            if (mode == TrainerMode.Drill) {
                copy(
                    moveResults = moveResults + (pathKey(ply + 1) to !currentMoveMissed),
                    lineMistakes = if (currentMoveMissed) lineMistakes + 1 else lineMistakes,
                )
            } else {
                this
            }
        return withResult.advanceTo(expected)
    }

    /** Plays the opponent's next move along the current line. Does nothing on the learner's turn. */
    fun playOpponentMove(): OpeningTrainer {
        if (!isOpponentTurn) return this
        return advanceTo(expectedGame ?: return this)
    }

    /**
     * Reveals more of the learner's next move in Drill mode: the piece first, then the full move.
     * Any hint marks the move as missed.
     */
    fun requestHint(): OpeningTrainer {
        if (mode != TrainerMode.Drill || !isLearnerTurn) return this
        val next = if (hint == HintLevel.None) HintLevel.Piece else HintLevel.Move
        return copy(hint = next, currentMoveMissed = true)
    }

    /** In Learn mode, goes back to just before the learner's previous move so it can be played again. */
    fun undo(): OpeningTrainer {
        if (mode != TrainerMode.Learn) return this
        val target = previousLearnerPly() ?: return this
        return copy(ply = target, game = replay(target), hint = HintLevel.None, currentMoveMissed = false)
    }

    /** Starts line [index] from the beginning, keeping the session's results. */
    fun selectLine(index: Int): OpeningTrainer {
        require(index in lines.indices) { "No line $index in ${opening.name}" }
        return copy(
            lineIndex = index,
            ply = 0,
            game = Game(),
            hint = HintLevel.None,
            currentMoveMissed = false,
            lineMistakes = 0,
        )
    }

    /**
     * Moves on to another line. Learn mode goes through the lines in order, wrapping around. Drill
     * mode picks a line not yet covered this session, or any line at random once all are covered.
     */
    fun nextLine(random: Random = Random): OpeningTrainer {
        if (lines.isEmpty()) return this
        return when (mode) {
            TrainerMode.Learn -> selectLine((lineIndex + 1) % lines.size)
            TrainerMode.Drill -> selectLine(pickDrillLine(random))
        }
    }

    /**
     * Switches to [newMode]. Learn restarts the current line; Drill starts a line chosen as in
     * [nextLine]. Results carry over.
     */
    fun withMode(
        newMode: TrainerMode,
        random: Random = Random,
    ): OpeningTrainer {
        if (lines.isEmpty()) return copy(mode = newMode)
        val switched = copy(mode = newMode)
        return when (newMode) {
            TrainerMode.Learn -> switched.selectLine(lineIndex)
            TrainerMode.Drill -> switched.selectLine(switched.pickDrillLine(random))
        }
    }

    private fun pickDrillLine(random: Random): Int {
        val uncovered = lines.indices.filter { it !in coveredLines }
        return if (uncovered.isNotEmpty()) uncovered.random(random) else lines.indices.random(random)
    }

    private fun advanceTo(next: Game): OpeningTrainer {
        val advanced = copy(ply = ply + 1, game = next, hint = HintLevel.None, currentMoveMissed = false)
        if (!advanced.isLineComplete || mode != TrainerMode.Drill) return advanced
        return advanced.copy(
            coveredLines = coveredLines + lineIndex,
            lineResults = lineResults + (pathKey(line.size) to (advanced.lineMistakes == 0)),
        )
    }

    private fun isLearnerPly(ply: Int): Boolean = (ply % 2 == 0) == (opening.side == PieceColor.White)

    private fun previousLearnerPly(): Int? = (ply - 1 downTo 0).firstOrNull { isLearnerPly(it) }

    private fun replay(toPly: Int): Game = line.take(toPly).fold(Game()) { game, node -> game.applySanMove(node.san!!) }

    private fun pathKey(plies: Int): String = line.take(plies).joinToString(" ") { it.san!! }

    companion object {
        /** Starts practising [opening] in [mode]: Learn from the first line, Drill from a random one. */
        fun start(
            opening: Opening,
            mode: TrainerMode = TrainerMode.Learn,
            random: Random = Random,
        ): OpeningTrainer = OpeningTrainer(opening).withMode(mode, random)
    }
}
