package com.bentrengrove.chess

class AI(val color: PieceColor) {
    // This AI doesn't work at all currently
    fun calculateNextMove(game: Game, player: PieceColor): Move? {
        return search(game, 0, player).getOrNull(0)?.first
    }

    fun search(game: Game, depth: Int, player: PieceColor): List<Pair<Move, Int>> {
        return game.allMovesFor(game.turn)
                .mapNotNull {
                    val newGame = game.doMove(it.from, it.to)
                    if (depth > 0 && game.turn == player) {
                        val best = search(newGame, depth - 1, player).firstOrNull()?.second ?: return@mapNotNull null
                        it to best
                    } else {
                        it to (newGame.valueFor(player) - newGame.valueFor(player.other()))
                    }
                }
                .sortedByDescending { it.second }
                .shuffled()
                .distinctBy { it.second }
                .toList()
    }
}