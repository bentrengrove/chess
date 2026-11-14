package com.bentrengrove.chess.gamescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bentrengrove.chess.engine.AI
import com.bentrengrove.chess.engine.Game
import com.bentrengrove.chess.engine.GameState
import com.bentrengrove.chess.engine.Move
import com.bentrengrove.chess.engine.MoveResult
import com.bentrengrove.chess.engine.PieceColor
import com.bentrengrove.chess.engine.PieceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _moveResult = MutableStateFlow<MoveResult>(MoveResult.Success(Game()))
    val moveResult: Flow<MoveResult> get() = _moveResult

    private var forwardHistory = MutableStateFlow<List<Move>>(listOf())

    val canGoBack =
        moveResult.map {
            val game = (_moveResult.value as? MoveResult.Success)?.game ?: return@map false
            return@map game.history.isNotEmpty()
        }
    val canGoForward = forwardHistory.map { it.isNotEmpty() }

    private val ai = AI(PieceColor.Black)
    private var aiEnabled = true

    fun updateResult(result: MoveResult) {
        _moveResult.tryEmit(result)

        val game = (result as? MoveResult.Success)?.game ?: return
        if (aiEnabled && game.turn == PieceColor.Black &&
            listOf(GameState.CHECK, GameState.IDLE).contains(game.gameState)
        ) {
            viewModelScope.launch {
                val nextMove = ai.calculateNextMove(game, PieceColor.Black)
                if (nextMove != null) {
                    val aiResult = game.doMove(nextMove.from, nextMove.to)
                    val finalAiResult =
                        when (aiResult) {
                            is MoveResult.Success -> aiResult
                            is MoveResult.Promotion -> aiResult.onPieceSelection(PieceType.Queen)
                        }
                    updateResult(finalAiResult)
                }
            }
        }
    }

    fun newGame(aiEnabled: Boolean) {
        this.aiEnabled = aiEnabled

        updateResult(MoveResult.Success(Game()))
        forwardHistory.tryEmit(listOf())
    }

    /**
     * Loads a game from FEN or PGN text as a two player game, positioned after its last move so
     * the player can step back through it. Returns false, leaving the current game untouched,
     * if the text can't be parsed.
     */
    fun loadGame(text: String): Boolean {
        val game =
            try {
                Game.fromFenOrPgn(text)
            } catch (e: Exception) {
                // Pasted text can fail in many ways (bad squares, illegal or truncated moves).
                return false
            }
        aiEnabled = false
        updateResult(MoveResult.Success(game))
        forwardHistory.tryEmit(listOf())
        return true
    }

    fun clearForwardHistory() {
        forwardHistory.tryEmit(listOf())
    }

    fun goBackMove() {
        val game = (_moveResult.value as? MoveResult.Success)?.game ?: return

        val lastMove = game.history.last()
        updateResult(MoveResult.Success(game.undoLastMove()))
        forwardHistory.tryEmit(forwardHistory.value + listOf(lastMove))
    }

    fun goForwardMove() {
        val game = (_moveResult.value as? MoveResult.Success)?.game ?: return

        val move = forwardHistory.value.last()
        forwardHistory.tryEmit(forwardHistory.value.subList(0, forwardHistory.value.size - 1))
        updateResult(MoveResult.Success(game.play(move)))
    }
}
