package com.bentrengrove.chess.openingscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bentrengrove.chess.engine.Opening
import com.bentrengrove.chess.engine.OpeningTrainer
import com.bentrengrove.chess.engine.Position
import com.bentrengrove.chess.engine.TrainerMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/** How long the opponent "thinks" before its move plays, so the learner can follow it. */
private const val OPPONENT_MOVE_DELAY_MS = 400L

/**
 * Owns an [OpeningTrainer] for one practice session and plays the opponent's replies after a
 * short delay. The session (covered lines and results) lasts as long as this view model.
 */
class OpeningPracticeViewModel(
    opening: Opening,
    private val random: Random = Random,
) : ViewModel() {
    private val _trainer = MutableStateFlow(OpeningTrainer.start(opening, TrainerMode.Learn, random))
    val trainer: StateFlow<OpeningTrainer> = _trainer.asStateFlow()

    private var opponentJob: Job? = null

    init {
        scheduleOpponentMove()
    }

    fun onMove(
        from: Position,
        to: Position,
    ) = update { it.playMove(from, to) }

    fun onHint() = update { it.requestHint() }

    fun onUndo() = update { it.undo() }

    fun onSelectLine(index: Int) = update { it.selectLine(index) }

    fun onNextLine() = update { it.nextLine(random) }

    fun onModeChange(mode: TrainerMode) {
        if (mode != _trainer.value.mode) update { it.withMode(mode, random) }
    }

    private fun update(action: (OpeningTrainer) -> OpeningTrainer) {
        val current = _trainer.value
        val next = action(current)
        if (next === current) return
        // A reply scheduled for the old position mustn't land on the new one.
        opponentJob?.cancel()
        _trainer.value = next
        scheduleOpponentMove()
    }

    private fun scheduleOpponentMove() {
        if (!_trainer.value.isOpponentTurn) return
        opponentJob =
            viewModelScope.launch {
                delay(OPPONENT_MOVE_DELAY_MS)
                _trainer.value = _trainer.value.playOpponentMove()
                scheduleOpponentMove()
            }
    }
}
