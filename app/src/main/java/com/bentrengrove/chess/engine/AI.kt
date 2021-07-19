package com.bentrengrove.chess.engine

class AI(val color: PieceColor) {
    // This AI doesn't work at all currently
    fun calculateNextMove(game: Game, player: PieceColor): Move? {
        return search(game, 0, player).getOrNull(0)?.first
    }

    fun search(game: Game, depth: Int, player: PieceColor): List<Pair<Move, Int>> {
        return game.allMovesFor(game.turn)
                .mapNotNull {
                    val moveResult = (game.doMove(it.from, it.to) as? MoveResult.Success)?.game ?: return@mapNotNull null

                    if (depth > 0 && game.turn == player) {
                        val best = search(moveResult, depth - 1, player).firstOrNull()?.second ?: return@mapNotNull null
                        it to best
                    } else {
                        it to (moveResult.valueFor(player) - moveResult.valueFor(player.other()))
                    }
                }
                .sortedByDescending { it.second }
                .shuffled()
                .distinctBy { it.second }
                .toList()
    }
}