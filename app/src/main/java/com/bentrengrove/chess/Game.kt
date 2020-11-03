package com.bentrengrove.chess

data class Move(val from: Position, val to: Position)

enum class GameState {
    IDLE, CHECK, CHECKMATE, STALEMATE
}

data class Game(var board: Board = Board(), val history: MutableList<Move> = mutableListOf()) {
    val turn: PieceColor
        get() = history.lastOrNull()?.let { board.pieceAt(it.to)?.color?.other() } ?: PieceColor.White

    fun canSelect(position: Position): Boolean {
        return board.pieceAt(position)?.color == turn
    }
}