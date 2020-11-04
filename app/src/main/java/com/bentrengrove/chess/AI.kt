package com.bentrengrove.chess

class AI(val color: PieceColor) {
    // This AI doesn't work at all currently
    fun calculateNextMove(game: Game): Move? {
        val moves = game.allMovesFor(color)
                .map { it to game.doMove(it.from, it.to).valueFor(color.other()) } // Minimise the opponents value
                .shuffled()
                .distinctBy { it.second }
                .sortedBy { it.second }
                .toList()

        return moves.getOrNull(0)?.first
    }
}