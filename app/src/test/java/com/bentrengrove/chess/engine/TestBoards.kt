package com.bentrengrove.chess.engine

/*
 * Test-only helpers for building custom positions from algebraic squares, since Board's
 * public constructor only exposes the raw [x][y] grid. Keeps the individual test files
 * readable ("e1" to "WK4") instead of hand-rolling 8x8 grids everywhere.
 */

/** Parses standard algebraic notation ("e4") into engine [Position] coordinates. */
fun sq(algebraic: String): Position {
    val file = algebraic[0] - 'a'
    val rank = algebraic[1] - '0'
    return Position(file, 8 - rank)
}

/** Builds a [Board] containing only the given pieces, everything else empty. */
fun customBoard(vararg placements: Pair<String, String>): Board {
    val grid = MutableList(8) { MutableList<Piece?>(8) { null } }
    placements.forEach { (squareId, pieceId) ->
        val position = sq(squareId)
        grid[position.y][position.x] = Piece.pieceFromString(pieceId)
    }
    return Board(grid.map { it.toList() })
}

/**
 * Builds a [Game] on a custom board whose [Game.turn] is forced to [colorToMove].
 *
 * Game.turn is derived from the color of the piece sitting on the last move's destination
 * square, so we synthesize a no-op-ish history entry landing on a real piece of the opposite
 * color already present on the board.
 */
fun customGame(
    board: Board,
    colorToMove: PieceColor,
    history: List<Move> = emptyList(),
): Game {
    val markerPiece = board.allPieces.firstOrNull { it.second.color == colorToMove.other() }
    val forcedHistory =
        if (markerPiece != null) {
            history + Move(from = markerPiece.first, to = markerPiece.first)
        } else {
            history
        }
    val game = Game(board = board, history = forcedHistory)
    check(game.turn == colorToMove) {
        "customGame setup failed to force turn=$colorToMove (got ${game.turn}); " +
            "board needs a ${colorToMove.other()} piece to anchor the turn-marker move"
    }
    return game
}
