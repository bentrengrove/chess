package com.bentrengrove.chess

import kotlin.math.abs

data class Move(val from: Position, val to: Position)

enum class GameState {
    IDLE, CHECK, CHECKMATE, STALEMATE
}

data class Game(val board: Board = Board(), val history: List<Move> = listOf()) {
    val gameState: GameState = GameState.IDLE

    val turn: PieceColor
        get() = history.lastOrNull()?.let { board.pieceAt(it.to)?.color?.other() } ?: PieceColor.White

    fun canSelect(position: Position): Boolean {
        return board.pieceAt(position)?.color == turn
    }

    fun canMove(from: Position, to: Position): Boolean {
        val piece = board.pieceAt(from) ?: return false

        val delta = to - from
        val other = board.pieceAt(to)
        if (other != null) {
            if (other.color == piece.color) {
                return false
            }
        }

        when (piece.type) {
            is PieceType.Pawn -> {
                if (delta.x != 0) {
                    return false
                }
                return when (piece.color) {
                    is PieceColor.White -> {
                        if (from.y == 6) {
                            listOf(-1, -2).contains(delta.y) &&
                                    !board.piecesExist(from, to)
                        } else {
                            delta.y == -1
                        }
                    }
                    is PieceColor.Black -> {
                        if (from.y == 1) {
                            listOf(1, 2).contains(delta.y) &&
                                    !board.piecesExist(from, to)
                        } else {
                            delta.y == 1
                        }
                    }
                }
            }
            is PieceType.Rook -> {
                return (delta.x == 0 || delta.y == 0) && !board.piecesExist(from, to)
            }
            is PieceType.Bishop -> {
                return abs(delta.x) == abs(delta.y) && !board.piecesExist(from, to)
            }
            is PieceType.Queen -> {
                return (delta.x == 0 || delta.y == 0 || abs(delta.x) == abs(delta.y)) && !board.piecesExist(from, to)
            }
            is PieceType.King -> {
                return abs(delta.x) <= 1 && abs(delta.y) <= 1
            }
            is PieceType.Knight -> {
                return listOf(
                    Delta(x = 1, y = 2),
                    Delta(x = -1, y = 2),
                    Delta(x = 2, y = 1),
                    Delta(x = -2, y = 1),
                    Delta(x = 1, y = -2),
                    Delta(x = -1, y = -2),
                    Delta(x = 2, y = -1),
                    Delta(x = -2, y = -1)
                ).contains(delta)
            }
        }
    }

    fun doMove(from: Position, to: Position): Game {
        return Game(board = board.movePiece(from, to), history = history + listOf(Move(from, to)))
    }

    fun movesForPieceAt(position: Position?): List<Position> {
        if (position == null) return emptyList()
        return board.allPositions.filter { canMove(position, it) }
    }
}

private fun Board.piecesExist(between: Position, and: Position): Boolean {
    val step = Delta(
        x = if(between.x > and.x) -1 else if (between.x < and.x) 1 else 0,
        y = if(between.y > and.y) -1 else if (between.y < and.y) 1 else 0
    )
    var position = between
    position += step
    while (position != and) {
        if (pieceAt(position) != null) {
            return true
        }
        position += step
    }
    return false
}